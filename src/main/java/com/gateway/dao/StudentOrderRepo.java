package com.gateway.dao;

import com.gateway.dto.StudentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentOrderRepo extends JpaRepository<StudentOrder,Integer> {

}
