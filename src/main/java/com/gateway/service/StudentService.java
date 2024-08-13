package com.gateway.service;

import com.gateway.dao.FailedPaymentRequestRepo;
import com.gateway.dao.StudentOrderRepo;
import com.gateway.dto.FailedPaymentRequest;
import com.gateway.dto.StudentOrder;
import com.gateway.exceptions.GatewayException;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.razorpay.RazorpayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    @Autowired
    private StudentOrderRepo studentOrderRepo;

    @Autowired
    private FailedPaymentRequestRepo failedPaymentRequestRepo;

    @Value("${razorpay.key.id}")
    private String razorPayKey;

    @Value("${razorpay.secret.key}")
    private String razorPaySecret;

    @Value("${payment.retry.initial-wait}")
    private int initialWait; // in minutes

    @Value("${payment.retry.max-wait}")
    private int maxWait; // in minutes

    @Value("${payment.retry.max-retries}")
    private int maxRetries;

    private RazorpayClient client;

    @Transactional
    public StudentOrder createOrder(StudentOrder studentOrder) throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        studentOrder.setIdempotencyKey(idempotencyKey); // Set the key in the order

        // Check if the transaction with the same idempotency key already exists
        StudentOrder existingOrder = studentOrderRepo.findByIdempotencyKey(idempotencyKey);
        if (existingOrder != null) {
            // Return the existing transaction status
            return existingOrder;
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", studentOrder.getAmount() * 100);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        this.client = new RazorpayClient(razorPayKey, razorPaySecret);

        int retryCount = 0;
        long waitTime = TimeUnit.MINUTES.toMillis(initialWait); // Convert initial wait to milliseconds

        while (retryCount < maxRetries) {
            try {
                // Create order for the request page
                Order razorPayOrder = client.orders.create(orderRequest);
                logger.info("Razorpay Order created: {}", razorPayOrder);

                // Getting the id from Razorpay and setting for storing in the database
                studentOrder.setRazorpayOrderId(razorPayOrder.get("id"));
                studentOrder.setOrderStatus(razorPayOrder.get("status"));

                // Saving the information in the database and returning the object
                return saveStudentOrder(studentOrder, idempotencyKey);
            } catch (RazorpayException | JSONException e) {
                retryCount++;
                if (handleRetry(retryCount, e, idempotencyKey, waitTime)) {
                    // Check if the order was saved before throwing the exception
                    StudentOrder retryOrder = studentOrderRepo.findByIdempotencyKey(idempotencyKey);
                    if (retryOrder != null) {
                        return retryOrder;
                    }
                    throw new RuntimeException("Error creating order with Razorpay: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                retryCount++;
                if (handleRetry(retryCount, e, idempotencyKey, waitTime)) {
                    // Check if the order was saved before throwing the exception
                    StudentOrder retryOrder = studentOrderRepo.findByIdempotencyKey(idempotencyKey);
                    if (retryOrder != null) {
                        return retryOrder;
                    }
                    throw new RuntimeException("Failed to create order after " + maxRetries + " attempts", e);
                }
            }
        }
        return null; // This line should never be reached due to the exception handling
    }

    private StudentOrder saveStudentOrder(StudentOrder studentOrder, String idempotencyKey) {
        try {
            return studentOrderRepo.save(studentOrder);
        } catch (Exception e) {
            // Log the error and rethrow the exception
            logger.error("Failed to save student order with idempotency key {}: {}", idempotencyKey, e.getMessage());
            throw e; // Rethrow to trigger retry logic
        }
    }

    private boolean handleRetry(int retryCount, Exception e, String idempotencyKey, long waitTime) throws GatewayException {
        if (retryCount >= maxRetries) {
            // Log and save the failed payment request
            FailedPaymentRequest failedPaymentRequest = new FailedPaymentRequest();
            failedPaymentRequest.setIdempotencyKey(idempotencyKey);
            failedPaymentRequest.setErrorMessage(e.getMessage());
            failedPaymentRequest.setTimestamp(LocalDateTime.now());
            failedPaymentRequestRepo.save(failedPaymentRequest);
            return true; // Indicate that max retries have been reached
        }

        if (e instanceof GatewayException) {
            GatewayException gatewayException = (GatewayException) e;

            if (gatewayException.isNetworkError()) {
                // Handle network errors, log, and retry
                logger.warn("Network error occurred: {}", e.getMessage());
            } else if (gatewayException.isPaymentError()) {
                // Handle payment-related errors, log, and retry
                logger.warn("Payment error occurred: {}", e.getMessage());
            } else {
                // Handle other types of errors
                logger.warn("An unexpected error occurred: {}", e.getMessage());
            }
        } else {
            // Log and retry for other general exceptions
            logger.warn("Retrying due to error: {}", e.getMessage());
        }

        // Calculate the next wait time
        waitTime = Math.min(waitTime * 1, TimeUnit.MINUTES.toMillis(maxWait)); // Exponential
        // backoff
        try {
            logger.info("Retrying in {} seconds...", waitTime / 1000);
            Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new GatewayException("Thread interrupted during backoff wait", ie);
        }
        return false; // Indicate that retries are still available
    }

    public StudentOrder updateOrder(Map<String, String> responsePayLoad) {
        if (responsePayLoad == null || !responsePayLoad.containsKey("razorpay_order_id")) {
            throw new IllegalArgumentException("Invalid payload: Missing razorpay_order_id");
        }

        String razorPayOrderId = responsePayLoad.get("razorpay_order_id");
        if (razorPayOrderId == null || razorPayOrderId.isEmpty()) {
            throw new IllegalArgumentException("Invalid razorpay_order_id");
        }

        StudentOrder orderData = studentOrderRepo.findByRazorpayOrderId(razorPayOrderId);

        if (orderData == null) {
            logger.warn("Order with Razorpay Order ID {} not found", razorPayOrderId);
            throw new RuntimeException("Order not found for Razorpay Order ID: " + razorPayOrderId);
        }

        // Update the order status
        orderData.setOrderStatus("PAYMENT_COMPLETED");

        try {
            StudentOrder updatedOrder = studentOrderRepo.save(orderData);
            logger.info("Order with Razorpay Order ID {} updated to PAYMENT_COMPLETED", razorPayOrderId);
            return updatedOrder;
        } catch (Exception e) {
            logger.error("Failed to update order with Razorpay Order ID {}: {}", razorPayOrderId, e.getMessage());
            throw new RuntimeException("Failed to update order", e);
        }
    }

}