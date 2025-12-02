package com.choco.home.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    @Value("${google.api.key}")
    private String googleApiKey;

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String q) {
        try {
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + encoded
                    + "&key=" + googleApiKey;

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/reverse")
    public ResponseEntity<String> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {

        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + lat + "," + lng
                    + "&key=" + googleApiKey;

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }
}

