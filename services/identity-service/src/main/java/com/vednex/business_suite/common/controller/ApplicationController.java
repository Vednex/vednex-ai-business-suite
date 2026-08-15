package com.vednex.business_suite.common.controller;


import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationController {

    private final String applicationName;

    public ApplicationController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/api/public/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(
                Map.of(
                        "application", "Vednex AI Business Suite",
                        "service", applicationName,
                        "version", "0.1.0",
                        "status", "RUNNING",
                        "timestamp", Instant.now()
                )
        );
    }
}
