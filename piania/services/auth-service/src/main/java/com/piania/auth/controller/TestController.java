package com.piania.auth.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/piania/test")
public class TestController {

    @GetMapping
    public String secured() {
        return "secured endpoint";
    }
}
