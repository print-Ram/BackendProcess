package com.choco.home.service;

import com.choco.home.pojo.Address;
import com.choco.home.pojo.Item;
import com.choco.home.pojo.Order;
import com.choco.home.pojo.Payment;
import com.choco.home.pojo.Product;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class OrderService {
	
	@Autowired
	private AddressService addressService;


    @Autowired
    private Firestore firestore;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private FirestoreService productService;
    
    private static final String ADDRESS_SUBCOLLECTION = "addresses"; // under users/{userId}
    private static final String PAYMENT_COLLECTION = "payments";
    private static final String COLLECTION_NAME = "orders";
    
    private static final String COUNTER_DOC = "orderCounter";
    private static final String METADATA_COLLECTION = "metadata";

    public String generateNextOrderId() throws ExecutionException, InterruptedException {
        DocumentReference counterRef = firestore.collection(METADATA_COLLECTION).document(COUNTER_DOC);

        ApiFuture<String> future = firestore.runTransaction(transaction -> {
            // Get the current document
            DocumentSnapshot snapshot = transaction.get(counterRef).get();

            // Check if the document exists; if not, create it with an initial value of 0
            long current;
            if (snapshot.exists()) {
                current = snapshot.contains("lastOrderNumber") ? snapshot.getLong("lastOrderNumber") : 0L;
            } else {
                // Create the document if it doesn't exist
                transaction.set(counterRef, Map.of("lastOrderNumber", 0L));
                current = 0L; // Starting the order count from 0
            }

            long next = current + 1;

            // Update the counter in the document
            transaction.update(counterRef, "lastOrderNumber", next);

            // Return the new order ID in the format "oid01", "oid02", etc.
            return String.format("oid%02d", next);
        });

        return future.get();
    }

    public int getOrderCountForUser(String userId) throws Exception {
        if (userId == null || userId.isEmpty()) return 0;

        CollectionReference ordersRef = firestore.collection("orders");

        Query query = ordersRef.whereEqualTo("userId", userId);
        ApiFuture<QuerySnapshot> snapshot = query.get();

        return ((Map) snapshot).size(); // number of orders user has placed
    }



    // Get all orders
    public List<Order> getAllOrders() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Order> orders = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
        	Order order = doc.toObject(Order.class);
            order.setOrder_id(doc.getId());
            orders.add(order);
        }
        return orders;
    }


    // Get orders by user ID
    public List<Order> getOrdersByUserId(String userId) throws Exception {
        CollectionReference ordersRef = firestore.collection("orders");
        Query query = ordersRef.whereEqualTo("user_id", userId);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        // Debug: Log the number of documents returned
        QuerySnapshot snapshot = querySnapshot.get();
        List<Order> userOrders = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Order order = doc.toObject(Order.class);
            userOrders.add(order);
        }

        return userOrders;
    }


    public Order getOrderByIdWithProductNames(String orderId) throws Exception {
        Order order = getOrderById(orderId);
        if (order == null) throw new RuntimeException("Order not found");

        if (order.getItems() != null) {
            for (Item item : order.getItems()) {
                Product product = productService.getProductById(item.getProduct_id());
                if (product != null) {
                    item.setProduct_name(product.getName()); // You need this setter
                }
            }
        }

        return order;
    }




    // Get order by ID
    public Order getOrderById(String orderId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(orderId);
        DocumentSnapshot doc = docRef.get().get();

        if (doc.exists()) {
            Order order = doc.toObject(Order.class);
            if (order != null) order.setOrder_id(doc.getId());
            return order;
        } else {
            return null;
        }
    }
    

 // Place a new order
    public String placeOrder(Order order) throws ExecutionException, InterruptedException {
        // Generate a custom sequential order ID like oid01, oid02, etc.
        order.setOrder_id(generateNextOrderId()); 

        // Create a new document in the "orders" collection with the custom order ID
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(order.getOrder_id());
        
        // Write the order data to Firestore
        ApiFuture<WriteResult> result = docRef.set(order);
        result.get(); // wait for write to complete

        // ✅ Send confirmation email to user
        String userId = order.getUser_id(); // Make sure this is set in the Order object
        if (userId != null && !userId.isEmpty()) {
            DocumentReference userRef = firestore.collection("users").document(userId);
            DocumentSnapshot userDoc = userRef.get().get();
            if (userDoc.exists()) {
                String email = userDoc.getString("email");
                if (email != null && !email.isEmpty()) {
                    emailService.sendOrderStatusEmail(email, order.getOrder_id(), "CONFIRMED");
                }
            }
        }

        return order.getOrder_id(); // Return the generated order ID
    }

    
    public String updateShippingDetails(
    	    String orderId,
    	    String status,
    	    String senderName,
    	    String courierService,
    	    String trackingId
    	) throws ExecutionException, InterruptedException {

    	    // Step 1: Fetch the order document
    	    DocumentReference orderRef = firestore.collection(COLLECTION_NAME).document(orderId);
    	    DocumentSnapshot orderDoc = orderRef.get().get();
    	    if (!orderDoc.exists()) {
    	        throw new RuntimeException("Order not found");
    	    }

    	    // Step 2: Get user ID from order
    	    String userId = orderDoc.getString("user_id");
    	    if (userId == null || userId.isEmpty()) {
    	        throw new RuntimeException("user_id not found in order");
    	    }

    	    // Step 3: Get user email
    	    DocumentReference userRef = firestore.collection("users").document(userId);
    	    DocumentSnapshot userDoc = userRef.get().get();
    	    if (!userDoc.exists()) {
    	        throw new RuntimeException("User not found for user_id: " + userId);
    	    }

    	    String email = userDoc.getString("email");
    	    if (email == null || email.isEmpty()) {
    	        throw new RuntimeException("Email not found for user_id: " + userId);
    	    }

    	    // Step 4: Update Firestore with shipping details + status
    	    Map<String, Object> updates = new HashMap<>();
    	    updates.put("status", status);
    	    updates.put("senderName", senderName);
    	    updates.put("courierService", courierService);
    	    updates.put("trackingId", trackingId);
    	    orderRef.update(updates).get();

    	    // Step 5: Prepare email content
    	    String shippingInfo = """
    	            Courier Service: %s
    	            Tracking ID: %s
    	            Dispatched by: %s
    	            """.formatted(courierService, trackingId, senderName);

    	    // Step 6: Send SHIPPED email with shipping info
    	    emailService.sendOrderStatusEmail(email, orderId, status, shippingInfo);

    	    return "Order with ID " + orderId + " updated to SHIPPED with sender details.";
    	}

    


    // Update order status
    public String updateOrderStatus(String orderId, String status) throws ExecutionException, InterruptedException {
        // Step 1: Fetch the order
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        DocumentSnapshot orderDoc = orderRef.get().get();
        if (!orderDoc.exists()) {
            throw new RuntimeException("Order not found");
        }

        // Step 2: Extract user_id
        String userId = orderDoc.getString("user_id");
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("user_id not found in order");
        }

        // Step 3: Fetch user to get email
        DocumentReference userRef = firestore.collection("users").document(userId);
        DocumentSnapshot userDoc = userRef.get().get();
        if (!userDoc.exists()) {
            throw new RuntimeException("User not found for user_id: " + userId);
        }

        String email = userDoc.getString("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found for user_id: " + userId);
        }

        // Step 4: Update status
        orderRef.update("status", status).get();

        // Step 5: Send email
        emailService.sendOrderStatusEmail(email, orderId, status);

        return "Order with ID " + orderId + " status updated to " + status + ".";
    }


    // Delete an order
    public String deleteOrder(String orderId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(orderId);
        ApiFuture<WriteResult> future = docRef.delete();
        future.get(); // wait for delete to complete
        return "Order with ID " + orderId + " deleted.";
    }
    
    
 // -------------------- ADDRESSES --------------------

    public String linkAddressToOrder(String orderId, String userId, String addressId)
            throws ExecutionException, InterruptedException {

        // Validate if address exists under this user
        List<Address> addresses = addressService.getAddressesForUser(userId);
        boolean found = addresses.stream().anyMatch(a -> a.getAddressId().equals(addressId));

        if (!found) {
            throw new RuntimeException("Address not found for user: " + userId);
        }

        // Update the order with the linked address ID
        DocumentReference orderRef = firestore.collection("orders").document(orderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("shippingAddressId", addressId);
        orderRef.update(updates).get();

        return "Address linked successfully to order " + orderId;
    }


    // -------------------- PAYMENTS --------------------

    /**
     * Create a payment record and optionally update the order with paymentId & paymentStatus.
     * Returns paymentId.
     */
    public String createPayment(Payment payment) throws ExecutionException, InterruptedException {
        String paymentId = UUID.randomUUID().toString();
        payment.setPaymentId(paymentId);
        payment.setCreatedAt(Timestamp.now());
        payment.setUpdatedAt(Timestamp.now());
        if (payment.getCurrency() == null) payment.setCurrency("INR");
        if (payment.getStatus() == null) payment.setStatus("INITIATED");

        DocumentReference payRef = firestore.collection(PAYMENT_COLLECTION).document(paymentId);
        payRef.set(payment).get();

        // update order document if orderId present
        if (payment.getOrderId() != null && !payment.getOrderId().isBlank()) {
            DocumentReference orderRef = firestore.collection(COLLECTION_NAME).document(payment.getOrderId());
            Map<String, Object> updates = new HashMap<>();
            updates.put("paymentId", paymentId);
            updates.put("paymentStatus", payment.getStatus());
            updates.put("paymentMethod", payment.getMethod());
            orderRef.update(updates).get();
        }

        return paymentId;
    }

    /**
     * Get payment by paymentId
     */
    public Payment getPaymentById(String paymentId) throws ExecutionException, InterruptedException {
        DocumentReference payRef = firestore.collection(PAYMENT_COLLECTION).document(paymentId);
        DocumentSnapshot doc = payRef.get().get();
        if (!doc.exists()) return null;
        Payment p = doc.toObject(Payment.class);
        return p;
    }

    /**
     * Get payment by orderId (returns first match or null)
     */
    public Payment getPaymentByOrderId(String orderId) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> f = firestore.collection(PAYMENT_COLLECTION)
                .whereEqualTo("orderId", orderId)
                .get();

        List<QueryDocumentSnapshot> docs = f.get().getDocuments();
        if (docs.isEmpty()) return null;
        return docs.get(0).toObject(Payment.class);
    }

    /**
     * Update payment status and update order's paymentStatus too.
     */
    public String updatePaymentStatus(String paymentId, String status, Map<String, Object> meta)
            throws ExecutionException, InterruptedException {

        DocumentReference payRef = firestore.collection(PAYMENT_COLLECTION).document(paymentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", Timestamp.now());
        if (meta != null && !meta.isEmpty()) updates.put("meta", meta);

        payRef.update(updates).get();

        // reflect to order if present
        Payment p = getPaymentById(paymentId);
        if (p != null && p.getOrderId() != null && !p.getOrderId().isBlank()) {
            DocumentReference orderRef = firestore.collection(COLLECTION_NAME).document(p.getOrderId());
            Map<String, Object> orderUpdates = new HashMap<>();
            orderUpdates.put("paymentStatus", status);
            orderRef.update(orderUpdates).get();
        }

        return "Payment status updated";
    }

    
    
    
    
}
