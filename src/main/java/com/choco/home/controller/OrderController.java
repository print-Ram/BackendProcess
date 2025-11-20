package com.choco.home.controller;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.choco.home.pojo.Order;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.choco.home.pojo.Order;
import com.choco.home.service.OrderService;
import com.razorpay.RazorpayClient;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;


    public OrderController() {

    }

    // ✅ USERS: Get only their own orders
    @GetMapping("/my")
    public ResponseEntity<List<Order>> getUserOrders(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }

        try {
            return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<?> getOrderById(HttpServletRequest request, @PathVariable String orderId) {
        String userId = (String) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        try {
            // Fetch raw order first
            Order order = orderService.getOrderById(orderId);

            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
            }

            // Role-based access control
            if ("USER".equals(role)) {
                // USER can only access their own orders
                if (!userId.equals(order.getUser_id())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
                }
            } else if (!"ADMIN".equals(role)) {
                // Only USER and ADMIN allowed
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            // After validation, fetch enriched order with product names
            Order enrichedOrder = orderService.getOrderByIdWithProductNames(orderId);

            return ResponseEntity.ok(enrichedOrder);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch order");
        }
    }

    
    @PostMapping("/update-shipping")
    public ResponseEntity<String> updateShipping(
        @RequestParam String orderId,
        @RequestParam String senderName,
        @RequestParam String courierService,
        @RequestParam String trackingId,
        HttpServletRequest request
    ) {
        // ✅ Check if user is ADMIN
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            String response = orderService.updateShippingDetails(
                orderId, "SHIPPED", senderName, courierService, trackingId
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    // ✅ ADMIN: Get all orders
    @GetMapping
    public ResponseEntity<?> getAllOrders(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            return ResponseEntity.ok(orderService.getAllOrders());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to get orders");
        }
    }

    // ✅ ADMIN: Update order status
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(HttpServletRequest request,
                                               @PathVariable String orderId,
                                               @RequestParam String status) {
    	if (orderId == null || orderId.isEmpty()) {
            return ResponseEntity.badRequest().body("Order ID cannot be empty");
        }
        String role = (String) request.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update status of " + orderId);
        }
    }

    // ✅ ADMIN: Delete an order
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(HttpServletRequest request, @PathVariable String orderId) {
        String role = (String) request.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            return ResponseEntity.ok(orderService.deleteOrder(orderId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to delete order.");
        }
    }

    // ✅ USERS: Place new order
    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(HttpServletRequest request, @RequestBody Order order) {
        String userId = (String) request.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
        	order.setUser_id(userId);
        	order.setStatus("CONFIRMED");
        	order.setOrderDate(new Date());

        	// Ensure shippingAddress is always an object
        	if (order.getAddressId() == null || order.getAddressId().isEmpty()) {
        	    return ResponseEntity.badRequest().body("Address ID is required for placing an order.");
        	}



            String orderId = orderService.placeOrder(order);
            return ResponseEntity.ok("Order placed successfully with ID: " + orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to place order");
        }
    }
    
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> payload) {
        try {
        	Object rawAmount = payload.get("amount");
            double amountDouble;

            if (rawAmount instanceof Integer) {
                amountDouble = ((Integer) rawAmount).doubleValue();
            } else if (rawAmount instanceof Double) {
                amountDouble = (Double) rawAmount;
            } else {
                return ResponseEntity.badRequest().body("Invalid amount type");
            }

            int amount = (int) (amountDouble * 100);


            RazorpayClient razorpay = new RazorpayClient("rzp_test_ZFRZgGGwyk6bgC", "eTXQfRrxnASyBQ2K9okdIqAA");

            JSONObject options = new JSONObject();
            options.put("amount", amount);
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + UUID.randomUUID().toString().substring(0, 8));

            com.razorpay.Order order = razorpay.orders.create(options);
            return ResponseEntity.ok(order.toString()); // contains id, amount, etc.

        } catch (Exception e) {
        	e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to create payment order.");
        }
    }
    
    @GetMapping("/get-address")
    public ResponseEntity<?> getAddress(@RequestParam double lat, @RequestParam double lng) {
        String apiKey = "AIzaSyD6e8uuOUS-REEZWnKUTk7osQAMNRizfmg";
        String url = String.format(
            "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s",
            lat, lng, apiKey
        );

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Geocoding failed");
        }
    }


    
}
