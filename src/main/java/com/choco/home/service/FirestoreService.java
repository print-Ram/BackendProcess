package com.choco.home.service;

import com.choco.home.pojo.Product;
import com.choco.home.pojo.Reply;
import com.choco.home.pojo.Review;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    @Autowired
    private Firestore firestore;
    
    private static final String REVIEW_COLLECTION = "reviews";
    private static final String COLLECTION_NAME = "products";
    private static final String METADATA_COLLECTION = "metadata";
    private static final String COUNTER_DOC = "productCounter";
    
    public String generateNextProductId() throws ExecutionException, InterruptedException {
        DocumentReference counterRef = firestore.collection(METADATA_COLLECTION).document(COUNTER_DOC);

        ApiFuture<String> future = firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(counterRef).get();

            long current;
            if (snapshot.exists()) {
                current = snapshot.contains("lastProductNumber") ? snapshot.getLong("lastProductNumber") : 0L;
            } else {
                transaction.set(counterRef, Map.of("lastProductNumber", 0L));
                current = 0L;
            }

            long next = current + 1;
            transaction.update(counterRef, "lastProductNumber", next);

            return String.format("khc%02d", next);
        });

        return future.get();
    }
    
    
    public void addCategoryToExistingProducts() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            DocumentReference docRef = doc.getReference();
            Map<String, Object> updates = new HashMap<>();
            updates.put("category", "general");  // Set default category

            docRef.update(updates); // Fire-and-forget or you can wait on the result if needed
        }
    }



    public List<Product> getAllProducts() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Product> products = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            Product product = doc.toObject(Product.class);
            product.setProduct_id(doc.getId()); // Set the document ID if needed
            products.add(product);
        }

        return products;
    }
    
    public List<Product> getProductsByName(String name) throws Exception {
        CollectionReference products = firestore.collection("products");

        // Assuming `name` field is indexed and stored in lowercase
        Query query = products
            .orderBy("name")
            .startAt(name.toLowerCase())
            .endAt(name.toLowerCase() + "\uf8ff");  // Firestore trick for "starts with"

        ApiFuture<QuerySnapshot> snapshot = query.get();

        List<Product> productList = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.get().getDocuments()) {
            productList.add(doc.toObject(Product.class));
        }
        return productList;
    }



    public Product getProductById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        DocumentSnapshot doc = docRef.get().get();
        if (doc.exists()) {
            Product product = doc.toObject(Product.class);
            if (product != null) product.setProduct_id(doc.getId());
            return product;
        } else {
            return null;
        }
    }

    public String addProduct(Product product) throws ExecutionException, InterruptedException {
        product.setProduct_id(generateNextProductId());
        product.setName(product.getName().toLowerCase());

        if (product.getCategory() == null || product.getCategory().isEmpty()) {
            product.setCategory("general");
        }

        // Save the product as-is (with optional fields)
        DocumentReference productRef = firestore.collection("products").document(product.getProduct_id());
        productRef.set(product).get();

        return product.getProduct_id();
    }





    public String updateProduct(String id, Product product) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        product.setProduct_id(id);
        product.setName(product.getName().toLowerCase());

        if (product.getCategory() == null || product.getCategory().isEmpty()) {
            product.setCategory("general");
        }

        docRef.set(product).get();

        return "Product with ID " + id + " updated.";
    }



    public String deleteProduct(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> future = docRef.delete();
        future.get(); // wait for delete to complete
        return "Product with ID " + id + " deleted.";
    }
    
    
 // ==================== REVIEW METHODS ====================

    public String addReview(Review review) throws ExecutionException, InterruptedException {
        String reviewId = UUID.randomUUID().toString();
        review.setReviewId(reviewId);
        review.setCreatedAt(Timestamp.now());
        review.setReplies(new ArrayList<>());

        firestore.collection(REVIEW_COLLECTION).document(reviewId).set(review).get();
        return reviewId;
    }

    public List<Review> getReviewsByProduct(String productId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(REVIEW_COLLECTION)
                .whereEqualTo("productId", productId)
                .get();

        List<QueryDocumentSnapshot> docs = future.get().getDocuments();
        List<Review> reviews = new ArrayList<>();

        for (QueryDocumentSnapshot doc : docs) {
            reviews.add(doc.toObject(Review.class));
        }

        return reviews;
    }

    public String deleteReview(String reviewId) throws ExecutionException, InterruptedException {
        firestore.collection(REVIEW_COLLECTION).document(reviewId).delete().get();
        return "Review deleted successfully";
    }

    public String addReplyToReview(String reviewId, Reply reply) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(REVIEW_COLLECTION).document(reviewId);

        reply.setReplyId(UUID.randomUUID().toString());
        reply.setCreatedAt(Timestamp.now());

        // Add reply to the array field
        ApiFuture<WriteResult> future = docRef.update("replies", FieldValue.arrayUnion(reply));
        future.get();

        return "Reply added successfully";
    }

}
