package com.choco.home.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String q) {

        try {
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=5&q=" + encoded;

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0"); // Nominatim requires a user-agent

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
