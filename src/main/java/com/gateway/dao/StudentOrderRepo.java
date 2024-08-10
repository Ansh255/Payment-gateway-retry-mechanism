package com.gateway.dao;

import com.gateway.dto.StudentOrderVO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentOrderRepo extends JpaRepository<StudentOrderVO,Integer> {

}
