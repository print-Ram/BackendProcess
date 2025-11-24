package com.choco.home.service;

import com.choco.home.pojo.Address;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class AddressService {

    @Autowired
    private Firestore firestore;

    private static final String USERS_COLLECTION = "users";
    private static final String ADDRESSES_SUBCOLLECTION = "addresses";

    /**
     * Save a new address under a specific user
     */
    public String saveAddressForUser(Address address) throws ExecutionException, InterruptedException {
        if (address.getUserId() == null || address.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID is required to save address");
        }

        // Create reference to subcollection: users/{userId}/addresses
        CollectionReference addressRef = firestore.collection(USERS_COLLECTION)
                .document(address.getUserId())
                .collection(ADDRESSES_SUBCOLLECTION);

        // Generate address ID if not provided
        String addressId = address.getAddressId() == null || address.getAddressId().isEmpty()
                ? UUID.randomUUID().toString()
                : address.getAddressId();

        address.setAddressId(addressId);
        address.setCreatedAt(Timestamp.now());

        ApiFuture<WriteResult> result = addressRef.document(addressId).set(address);
        result.get(); // wait for write

        return addressId;
    }

    /**
     * Get all saved addresses for a given user
     */
    public List<Address> getAddressesForUser(String userId) throws ExecutionException, InterruptedException {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        CollectionReference addressRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ADDRESSES_SUBCOLLECTION);

        ApiFuture<QuerySnapshot> future = addressRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Address> addresses = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            Address address = doc.toObject(Address.class);
            if (address != null) {
                addresses.add(address);
            }
        }

        return addresses;
    }

    public void updateAddress(String addressId, Address address) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // Ensure the address ID stays consistent
        address.setAddressId(addressId);

        ApiFuture<WriteResult> future = db
                .collection("addresses")
                .document(addressId)
                .set(address);  // .set() replaces the document

        future.get(); // block until completed
    }

    
    /**
     * Delete an address for a specific user
     */
    public String deleteAddressForUser(String userId, String addressId) throws ExecutionException, InterruptedException {
        if (userId == null || addressId == null) {
            throw new IllegalArgumentException("User ID and Address ID cannot be null");
        }

        DocumentReference addressDocRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ADDRESSES_SUBCOLLECTION)
                .document(addressId);

        ApiFuture<WriteResult> writeResult = addressDocRef.delete();
        writeResult.get(); // wait for delete

        return "Address with ID " + addressId + " deleted for user " + userId;
    }

    /**
     * Set an address as default for a user
     */
    public String setDefaultAddress(String userId, String addressId) throws ExecutionException, InterruptedException {
        CollectionReference addressRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ADDRESSES_SUBCOLLECTION);

        // Step 1: Unset default for all addresses
        ApiFuture<QuerySnapshot> future = addressRef.get();
        for (DocumentSnapshot doc : future.get().getDocuments()) {
            doc.getReference().update("isDefault", false);
        }

        // Step 2: Set default for selected address
        DocumentReference defaultRef = addressRef.document(addressId);
        defaultRef.update("isDefault", true).get();

        return "Address " + addressId + " set as default for user " + userId;
    }
}
