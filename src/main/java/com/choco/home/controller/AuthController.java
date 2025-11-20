package com.choco.home.controller;

import com.choco.home.pojo.User;
import com.choco.home.security.JwtTokenProvider;
import com.choco.home.service.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import com.choco.home.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserServiceImpl userServiceImpl;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public AuthController(UserServiceImpl userServiceImpl,
                          JwtTokenProvider jwtTokenProvider,
                          UserService userService) {
        this.userServiceImpl = userServiceImpl;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    // Register user with auto-generated ID and default role USER
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User userRequest) {
        try {
        	
        	// Check if email already exists
            Optional<User> existingUser = userServiceImpl.findByEmail(userRequest.getEmail());
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered.");
            }
        	
            String userId = userServiceImpl.generateNextUserId();
            userRequest.setUser_id(userId);
            userRequest.setRole("USER"); // auto-assign user role

            userService.saveUser(userRequest);

            // ✅ Generate token with userId and role
            String token = jwtTokenProvider.generateToken(userId, "USER");

            Map<String, String> response = new HashMap<>();
            response.put("userId", userId);
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(500).body("Failed to register user: " + e.getMessage());
        }
    }



    // Login/authenticate user
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User user = userService.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            // ✅ Generate token with userId and role
        	String token = jwtTokenProvider.generateToken(user.getUser_id(), user.getRole());

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole());
            response.put("userId", user.getUser_id());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    //Get all users 
    @GetMapping
    public ResponseEntity<?> getAllusers(HttpServletRequest request) {
    	String role = (String) request.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
    	
        try {
        	return ResponseEntity.ok(userServiceImpl.getAllusers());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to get users");
        }
    }
    
 // For customers: Get user details by userId
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String tokenUserId = (String) request.getAttribute("userId");

        // ✅ Allow if admin OR if user is accessing their own profile
        if (!"ADMIN".equals(role) && !userId.equals(tokenUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            return ResponseEntity.ok(userServiceImpl.getUserById(userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to get user details");
        }
    }

    
    @PutMapping("/{userId}/update")
    public ResponseEntity<?> updateUser(@PathVariable String userId,
                                        @RequestBody User updatedUser,
                                        HttpServletRequest request) throws ExecutionException, InterruptedException {
        String role = (String) request.getAttribute("role");
        String tokenUserId = (String) request.getAttribute("userId");

        // ✅ Allow if admin OR user updating their own profile
        if (!"ADMIN".equals(role) && !userId.equals(tokenUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            String result = userServiceImpl.updateUserFields(userId, updatedUser);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update user");
        }
    }


    
    
 // For admin: Delete a user
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId,HttpServletRequest request) {
    	String role = (String) request.getAttribute("role");
    	if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            return ResponseEntity.ok(userServiceImpl.deleteUser(userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to delte user");
        }
    }

}
