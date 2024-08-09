package com.gateway.controller;

import com.gateway.dto.StudentOrder;
import com.gateway.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class StudentController {

    @GetMapping("/")
    public String init(){
        return "index";
    }

//    private StudentOrder studentOrder;

    @Autowired
    private StudentService studentService;


}
