package com.gateway.controller;

import com.gateway.dto.StudentOrder;
import com.gateway.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class StudentController {

    @Autowired
    private StudentService studentService;



    @GetMapping("/")
    public String init(){
        return "index";
    }

    @PostMapping(value = "/create-order" ,produces = "application/json")
    @ResponseBody
    public ResponseEntity<StudentOrder> createOrder(@RequestBody StudentOrder studentOrder)throws Exception{
        StudentOrder createdOrder = studentService.createOrder(studentOrder);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @PostMapping(value = "/handle-payment-callback")
    public String handlePaymentCallBack(@RequestParam Map<String,String> responsePayLoad) {
        System.out.println(responsePayLoad);
        StudentOrder updateOrder = studentService.updateOrder(responsePayLoad);
        System.out.println("Updated Order"+ updateOrder);
        return "success";
    }
}
