package com.gateway.controller;

import com.gateway.dto.StudentOrderVO;
import com.gateway.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/")
    public ModelAndView loadIndex(){
        return new ModelAndView("index","StudentOrderVO",new StudentOrderVO());
    }

    @PostMapping(value = "processing-payment")
    public ModelAndView createOrder(@ModelAttribute StudentOrderVO studentOrderVO) {
        return new ModelAndView("success");
    }
}
