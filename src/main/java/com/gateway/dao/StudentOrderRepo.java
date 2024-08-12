package com.gateway.dao;
import com.gateway.dto.StudentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface StudentOrderRepo extends JpaRepository<StudentOrder,Integer> {

    StudentOrder findByRazorpayOrderId(String orderId);
    boolean existsByIdempotencyKey(String idempotencyKey);
    StudentOrder findByIdempotencyKey(String idempotencyKey);
}
