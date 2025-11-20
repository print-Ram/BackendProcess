package com.choco.home.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.choco.home.pojo.Product;
import com.choco.home.service.FirestoreService;

import jakarta.servlet.http.HttpServletRequest;

@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", allowCredentials = "true")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private FirestoreService firestoreService;

    @GetMapping
    public List<Product> getAllProducts() {
        try {
//        	System.out.println("Fetching all products...");
            return firestoreService.getAllProducts();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable String id) {
        try {
            return firestoreService.getProductById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @GetMapping("/search/{name}")
    public List<Product> getProductsByName(@PathVariable String name) {
        try {
            return firestoreService.getProductsByName(name);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @PostMapping("/auth/add_item")
    public ResponseEntity<?> addProduct(HttpServletRequest request, @RequestBody Product product) {
        String role = (String) request.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            String productId = firestoreService.addProduct(product); // generates and sets product_id internally
            return ResponseEntity.ok(Collections.singletonMap("productId", productId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to add product");
        }
    }

    @PutMapping("/auth/update_item/{id}")
    public ResponseEntity<?> updateProduct(HttpServletRequest request, @PathVariable String id, @RequestBody Product product) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            firestoreService.updateProduct(id, product);
            return ResponseEntity.ok(Map.of("message", "Product updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update product"));
        }
    }

    @DeleteMapping("/auth/delete_item/{id}")
    public ResponseEntity<?> deleteProduct(HttpServletRequest request, @PathVariable String id) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            return ResponseEntity.ok(firestoreService.deleteProduct(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to delete product");
        }
    }

}
