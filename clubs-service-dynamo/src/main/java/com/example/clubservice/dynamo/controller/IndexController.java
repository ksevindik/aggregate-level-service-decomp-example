package com.example.clubservice.dynamo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class IndexController {
    @GetMapping("/")
    public String getIndexPage() {
        return "/swagger-ui.html";
    }
}