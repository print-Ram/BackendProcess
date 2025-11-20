package com.choco.home.controller;

import com.choco.home.pojo.Review;
import com.choco.home.pojo.Reply;
import com.choco.home.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private FirestoreService firestoreService;

    @PostMapping
    public ResponseEntity<String> addReview(@RequestBody Review review) throws ExecutionException, InterruptedException {
        String id = firestoreService.addReview(review);
        return ResponseEntity.ok("Review added successfully with ID: " + id);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getReviews(@PathVariable String productId)
            throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(firestoreService.getReviewsByProduct(productId));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable String reviewId)
            throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(firestoreService.deleteReview(reviewId));
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<String> addReply(@PathVariable String reviewId, @RequestBody Reply reply)
            throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(firestoreService.addReplyToReview(reviewId, reply));
    }
}
