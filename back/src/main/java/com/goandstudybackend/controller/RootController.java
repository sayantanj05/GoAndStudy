package com.goandstudybackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping({"/", "/api"})
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "GoAndStudy Backend",
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "health", "/api/v1/health",
                "auth", "/api/v1/auth/login",
                "docs", "/swagger-ui.html"
        ));
    }
}
