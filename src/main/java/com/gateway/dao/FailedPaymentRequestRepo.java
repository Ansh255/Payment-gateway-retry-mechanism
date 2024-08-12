package com.gateway.dao;

import com.gateway.dto.FailedPaymentRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

@Transactional
public interface FailedPaymentRequestRepo extends JpaRepository<FailedPaymentRequest, Long> {
}
