package com.chimera.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/health")
    public String health() {
        return "Project Chimera is alive! Running on Java 21 with VS Code.";
    }
}
