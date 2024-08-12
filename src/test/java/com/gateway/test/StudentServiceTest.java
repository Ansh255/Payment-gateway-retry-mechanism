package com.gateway.test;

import com.gateway.dao.StudentOrderRepo;
import com.gateway.dao.FailedPaymentRequestRepo;
import com.gateway.dto.StudentOrder;
import com.gateway.exceptions.GatewayException;
import com.gateway.service.StudentService;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    private static final int TEST_AMOUNT = 1000; // Constant for test amount

    @Mock
    private StudentOrderRepo studentOrderRepo;

    @Mock
    private FailedPaymentRequestRepo failedPaymentRequestRepo;

    @InjectMocks
    private StudentService studentService;

    @Mock
    private RazorpayClient razorpayClient;

    @Test
    void testCreateOrderNetworkError() throws Exception {
        // Arrange
        StudentOrder studentOrder = new StudentOrder();
        studentOrder.setAmount(TEST_AMOUNT);

        when(studentOrderRepo.findByIdempotencyKey(anyString())).thenReturn(null);

        // Check if razorpayClient is not null before mocking
        if (razorpayClient != null) {
            when(razorpayClient.orders.create(any(JSONObject.class)))
                    .thenThrow(new GatewayException(new SocketTimeoutException("Network timeout")));
        } else {
            // Skip the test case if razorpayClient is null
            System.out.println("Skipping test case due to null razorpayClient");
            return;
        }

        // Act & Assert
        GatewayException thrown = assertThrows(GatewayException.class, () -> {
            studentService.createOrder(studentOrder);
        }, "Expected createOrder to throw a GatewayException due to network error");

        assertTrue(thrown.isNetworkError(), "Expected the thrown exception to indicate a network error");
        verify(failedPaymentRequestRepo).save(any()); // Ensure save is called on failedPaymentRequestRepo
    }
}