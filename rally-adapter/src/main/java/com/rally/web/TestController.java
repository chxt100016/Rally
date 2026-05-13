package com.rally.web;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@RestController
@Slf4j
@RequestMapping("/test")
public class TestController {



    @RequestMapping("/hello")
    public String hello() {
        return "hello world";
    }


}
