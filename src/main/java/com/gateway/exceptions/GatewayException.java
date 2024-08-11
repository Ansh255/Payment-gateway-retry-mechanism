package com.gateway.exceptions;

import com.razorpay.RazorpayException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Custom exception class for handling gateway-related exceptions.
 */
public class GatewayException extends Exception {

    public GatewayException(String message) {
        super(message);
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public GatewayException(Throwable cause) {
        super(cause);
    }

    /**
     * Checks if the exception is related to network errors.
     * @return true if the exception is caused by a network-related issue, false otherwise.
     */
    public boolean isNetworkError() {
        return isCausedBy(SocketTimeoutException.class, UnknownHostException.class, TimeoutException.class, IOException.class);
    }

    /**
     * Checks if the exception is related to payment errors.
     * @return true if the exception is caused by payment-related issues, false otherwise.
     */
    public boolean isPaymentError() {
        if (isCausedBy(RazorpayException.class)) {
            Throwable cause = this.getCause();
            while (cause != null) {
                if (cause instanceof RazorpayException) {
                    String message = cause.getMessage().toLowerCase();
                    if (message.contains("payment") ||
                            message.contains("insufficient funds") ||
                            message.contains("card expired") ||
                            message.contains("payment declined")) {
                        return true;
                    }
                }
                cause = cause.getCause();
            }
        }
        return false;
    }

    /**
     * Utility method to check if the cause of the exception is of a specific type.
     * @param causeClasses the classes to check against.
     * @return true if the cause matches any of the specified classes, false otherwise.
     */
    private boolean isCausedBy(Class<?>... causeClasses) {
        Throwable cause = this.getCause();
        while (cause != null) {
            for (Class<?> causeClass : causeClasses) {
                if (causeClass.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}