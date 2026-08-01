package br.com.orcamento3d.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/public/health")
    Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
