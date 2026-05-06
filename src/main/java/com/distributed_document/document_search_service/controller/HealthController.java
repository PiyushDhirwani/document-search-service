package com.distributed_document.document_search_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "document-search-service");

        // Check MySQL
        try (Connection conn = dataSource.getConnection()) {
            health.put("mysql", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("mysql", "DOWN");
            health.put("mysql_error", e.getMessage());
        }

        // Check Redis
        try {
            redisConnectionFactory.getConnection().ping();
            health.put("redis", "UP");
        } catch (Exception e) {
            health.put("redis", "DOWN");
            health.put("redis_error", e.getMessage());
        }

        boolean allUp = "UP".equals(health.get("mysql")) && "UP".equals(health.get("redis"));
        health.put("status", allUp ? "UP" : "DEGRADED");

        return ResponseEntity.ok(health);
    }
}
