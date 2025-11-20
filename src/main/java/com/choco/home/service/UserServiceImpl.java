package com.choco.home.service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.choco.home.pojo.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private Firestore firestore;

    private static final String COLLECTION_NAME = "users";
    private static final String METADATA_COLLECTION = "metadata";
    private static final String COUNTER_DOC = "userCounter";
    
    @Override
    public void saveUser(User user) {
        try {
            adduser(user);  // method already writes to Firestore and assigns userId
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage());
        }
    }


    @Override
    public User findByUsername(String username) {
        try {
            CollectionReference users = firestore.collection(COLLECTION_NAME);
            Query query = users.whereEqualTo("email", username); // Assuming email is used as username
            ApiFuture<QuerySnapshot> querySnapshot = query.get();

            for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
                return doc.toObject(User.class);  // Return the first matching user
            }
            return null;  // No user found
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user: " + e.getMessage());
        }
    }
    
    
    //generateNextUserId
    public String generateNextUserId() throws ExecutionException, InterruptedException {
        DocumentReference counterRef = firestore.collection(METADATA_COLLECTION).document(COUNTER_DOC);

        ApiFuture<String> future = firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(counterRef).get();

            long current;
            if (snapshot.exists()) {
                current = snapshot.contains("lastUserNumber") ? snapshot.getLong("lastUserNumber") : 0L;
            } else {
                transaction.set(counterRef, Map.of("lastUserNumber", 0L));
                current = 0L;
            }

            long next = current + 1;
            transaction.update(counterRef, "lastUserNumber", next);

            return String.format("uid%02d", next);
        });

        return future.get();
    }

    
    // Get all users
    public List<User> getAllusers() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<User> users = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            User user = doc.toObject(User.class);
            user.setUser_id(doc.getId());
            users.add(user);
        }
        return users;
    }
    
    // Get user by ID
    public User getUserById(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
        DocumentSnapshot doc = docRef.get().get();

        if (doc.exists()) {
            User user = doc.toObject(User.class);
            if (user != null) user.setUser_id(doc.getId());
            return user;
        } else {
            return null;
        }
    }
    
    public String updateUserFields(String userId, User updatedUser) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
        Map<String, Object> updates = new java.util.HashMap<>();

        if (updatedUser.getName() != null) {
            updates.put("name", updatedUser.getName());
        }
        if (updatedUser.getPassword() != null) {
            updates.put("password", updatedUser.getPassword());
        }
        if (updatedUser.getAddress() != null) {
            updates.put("address", updatedUser.getAddress());
        }
        if (updatedUser.getContact() != null) {
            updates.put("contact", updatedUser.getContact());
        }

        if (updates.isEmpty()) {
            return "No fields to update.";
        }

        ApiFuture<WriteResult> future = docRef.update(updates);
        future.get(); // Wait for completion

        return "User fields updated successfully.";
    }

    
    
    // add a new user
    public String adduser(User user) throws ExecutionException, InterruptedException {
        // Generate a custom sequential user ID like uid01, uid02, etc.
        user.setUser_id(generateNextUserId()); 

        // Create a new document in the "users" collection with the custom user ID
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(user.getUser_id());
        
        // Write the user data to Firestore
        ApiFuture<WriteResult> result = docRef.set(user);
        result.get(); // wait for write to complete

        return user.getUser_id(); // Return the generated user ID
    }
    
    //Find Unique users
    public Optional<User> findByEmail(String email) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("users")
            .whereEqualTo("email", email)
            .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (documents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(documents.get(0).toObject(User.class));
    }
    
    
    // Delete an user
    public String deleteUser(String userId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(userId);
        ApiFuture<WriteResult> future = docRef.delete();
        future.get(); // wait for delete to complete
        return "Order with ID " + userId + " deleted.";
    }
    
}
