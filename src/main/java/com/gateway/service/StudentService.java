package com.gateway.service;

import com.gateway.dao.StudentOrderRepo;
import com.gateway.dto.StudentOrder;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class StudentService {

    @Autowired
    private StudentOrderRepo studentOrderRepo;

    @Value("${razorpay.key.id}")
    private String razorPayKey;

    @Value("${razorpay.secret.key}")
    private String razorPaySecret;

    private RazorpayClient client;

    private static final long INITIAL_BACKOFF = 2000; // 2 seconds
    private static final int MAX_RETRIES = 3;
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);
    private static final BlockingQueue<StudentOrder> manualReviewQueue = new LinkedBlockingQueue<>();

    // Constructor to initialize RazorpayClient
    public StudentService() {
        try {
            this.client = new RazorpayClient(razorPayKey, razorPaySecret);
        } catch (Exception e) {
            logger.error("Error initializing Razorpay client: " + e.getMessage(), e);
        }
    }

    // Method to create an order
    public StudentOrder createOrder(StudentOrder studentOrderVO) throws Exception {
        // Create a new StudentOrder instance from the VO
        StudentOrder studentOrder = new StudentOrder();
        studentOrder.setAmount(studentOrderVO.getAmount());
        studentOrder.setEmail(studentOrderVO.getEmail());

        // Prepare the order request
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", studentOrder.getAmount() * 100); // Amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());
        //Razorpay automatically checks for idempotency key
        String idempotencyKey = UUID.randomUUID().toString(); // Generate a unique key
        orderRequest.put("idempotency_key", idempotencyKey);

        int attempt = 0;
        long backoffTime = INITIAL_BACKOFF;

        while (attempt < MAX_RETRIES) {
            try {
                // Attempt to create the order
                Order razorPayOrder = client.orders.create(orderRequest);
                studentOrder.setRazorpayOrderId(razorPayOrder.get("id"));
                studentOrder.setOrderStatus(razorPayOrder.get("status"));
                studentOrderRepo.save(studentOrder); // Save the order in the database
                notifyUser("Payment successful!");
                manualReviewQueue.add(razorPayOrder.get("id"));
                System.out.println("Queue"+manualReviewQueue);
                return studentOrder; // Return the created order

            } catch (Exception e) {
                // Handle other exceptions
                logger.error("Unexpected error on attempt " + attempt + ": " + e.getMessage(), e);
                throw e;
            }
        }

        // Fallback return in case of failure
        return null;
    }

    // Method to update the order status after payment
    public StudentOrder updateOrder(Map<String, String> responsePayLoad) {
        String razorPayOrderId = responsePayLoad.get("razorpay_order_id");
        StudentOrder orderData = studentOrderRepo.findByRazorpayOrderId(razorPayOrderId);
        orderData.setOrderStatus("PAYMENT_COMPLETED");
        return studentOrderRepo.save(orderData); // Save and return updated order
    }

    // Method to notify user (e.g., via logging or sending a message)
    private void notifyUser(String message) {
        // Log the message or send it to the frontend
        logger.info(message);
        // Optionally, add more complex notification logic here
    }

    // Method to add orders to the manual review queue
    private void queueForManualReview(StudentOrder studentOrder) {
        manualReviewQueue.add(studentOrder);
        logger.info("Order queued for manual review: " + studentOrder.getRazorpayOrderId());
    }
}
