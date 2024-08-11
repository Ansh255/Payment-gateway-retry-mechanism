package com.gateway.dao;

import com.gateway.dto.FailedPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FailedPaymentRequestRepo extends JpaRepository<FailedPaymentRequest, Long> {
}
