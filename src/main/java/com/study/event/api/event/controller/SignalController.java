package com.study.event.api.event.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignalController {

    @GetMapping("/api/signal")
    public String signal() {
        return "Signal Endpoint";
    }
}
