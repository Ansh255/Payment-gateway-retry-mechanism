package com.gateway.service;

import com.gateway.dao.FailedPaymentRequestRepo;
import com.gateway.dao.StudentOrderRepo;
import com.gateway.dto.FailedPaymentRequest;
import com.gateway.dto.StudentOrder;
import com.razorpay.Order;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.razorpay.RazorpayClient;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {

    //Autowiring section
    @Autowired
    private StudentOrderRepo studentOrderRepo;

    @Autowired
    private FailedPaymentRequestRepo failedPaymentRequestRepo;

    //Configuration section for payment
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

    //Service method for custom logic
    public StudentOrder createOrder(StudentOrder studentOrder) throws Exception {

        String idempotencyKey = UUID.randomUUID().toString();
        studentOrder.setIdempotencyKey(idempotencyKey); // Set the key in the order


        // Check if the transaction with the same idempotency key already exists
        if (studentOrderRepo.existsByIdempotencyKey(idempotencyKey)) {
            // Return the existing transaction status
            return studentOrderRepo.findByIdempotencyKey(idempotencyKey);
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
                System.out.println(razorPayOrder);

                // Getting the id from Razorpay and setting for storing in the database
                studentOrder.setRazorpayOrderId(razorPayOrder.get("id"));
                studentOrder.setOrderStatus(razorPayOrder.get("status"));

                // Saving the information in the database and returning the object
                studentOrderRepo.save(studentOrder);
                return studentOrder; // Return successful order creation
            }
            catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    //for failed transactions
                    FailedPaymentRequest failedPaymentRequest = new FailedPaymentRequest();
                    failedPaymentRequest.setIdempotencyKey(idempotencyKey);
                    failedPaymentRequest.setErrorMessage(e.getMessage());
                    failedPaymentRequest.setTimestamp(LocalDateTime.now());
                    failedPaymentRequestRepo.save(failedPaymentRequest);
                    throw new RuntimeException("Failed to create order after " + maxRetries + " attempts", e);
                }
                // Calculate the next wait time
                waitTime = Math.min(waitTime * 1, TimeUnit.MINUTES.toMillis(maxWait));
                try {
                    // Wait before retrying
                    System.out.println("Retrying in " + (waitTime / 1000) + " seconds...");
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    throw new RuntimeException("Thread interrupted during backoff wait", ie);
                }
            }
        }
        return null; // This line should never be reached due to the exception handling
    }

    public StudentOrder updateOrder(Map<String, String> responsePayLoad) {
        String razorPayOrderId = responsePayLoad.get("razorpay_order_id");
        StudentOrder orderData = studentOrderRepo.findByRazorpayOrderId(razorPayOrderId);
        orderData.setOrderStatus("PAYMENT_COMPLETED");
        StudentOrder updatedOrder = studentOrderRepo.save(orderData);
        return updatedOrder;
    }
}