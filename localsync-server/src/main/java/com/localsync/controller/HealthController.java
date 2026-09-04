package com.localsync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        String host = "unknown";
        try { host = InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "LocalSync Server",
            "version", "1.0.0",
            "host", host
        ));
    }
}
