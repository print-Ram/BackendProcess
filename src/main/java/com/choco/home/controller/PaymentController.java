package com.choco.home.controller;

import com.choco.home.pojo.Payment;
import com.choco.home.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<String> createPayment(@RequestBody Payment payment) throws ExecutionException, InterruptedException {
        String id = orderService.createPayment(payment);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) throws ExecutionException, InterruptedException {
        Payment p = orderService.getPaymentById(paymentId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(@PathVariable String orderId) throws ExecutionException, InterruptedException {
        Payment p = orderService.getPaymentByOrderId(orderId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<String> updatePaymentStatus(@PathVariable String paymentId,
                                                      @RequestBody Map<String, Object> body)
            throws ExecutionException, InterruptedException {
        String status = (String) body.get("status");
        Map<String, Object> meta = (Map<String, Object>) body.get("meta");
        return ResponseEntity.ok(orderService.updatePaymentStatus(paymentId, status, meta));
    }
}
