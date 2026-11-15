package com.example.blackjack;

import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
public class LocalEnvironmentController {
    private final JdbcTemplate jdbc;

    public LocalEnvironmentController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // "/" はアプリのトップ（HomeController）が使うので、確認用は /api/dev/health だけにする
    @GetMapping("/api/dev/health")
    public Map<String, String> health() {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        if (!Integer.valueOf(1).equals(result)) {
            throw new IllegalStateException("Database connection check failed");
        }
        return Map.of("application", "blackjack", "status", "UP", "database", "CONNECTED");
    }
}
