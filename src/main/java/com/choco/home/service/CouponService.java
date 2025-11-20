package com.choco.home.service;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.choco.home.dto.ApplyCouponRequest;
import com.choco.home.dto.ApplyCouponResponse;
import com.choco.home.dto.CartItemDto;
import com.choco.home.pojo.Coupon;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

@Service
public class CouponService {
	@Autowired
    private OrderService orderService;

    private static final String COLLECTION_NAME = "coupons";

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    // ========= ADMIN FUNCTIONS =========

    public Coupon createCoupon(Coupon coupon) throws ExecutionException, InterruptedException {
        coupon.setId(UUID.randomUUID().toString());
        coupon.setCreatedAt(new Date());
        coupon.setActive(true);

        ApiFuture<WriteResult> result = db().collection(COLLECTION_NAME).document(coupon.getId()).set(coupon);
        result.get(); // wait for write
        return coupon;
    }

    public List<Coupon> getAllCoupons() throws ExecutionException, InterruptedException {
        List<Coupon> coupons = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db().collection(COLLECTION_NAME).get();
        for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
            coupons.add(doc.toObject(Coupon.class));
        }
        return coupons;
    }

    public Coupon updateCoupon(String id, Coupon updated) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db().collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> snapshot = docRef.get();
        if (!snapshot.get().exists()) throw new RuntimeException("Coupon not found!");

        updated.setId(id);
        docRef.set(updated);
        return updated;
    }

    public String deleteCoupon(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db().collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> snapshot = docRef.get();
        if (!snapshot.get().exists()) throw new RuntimeException("Coupon not found!");
        docRef.delete();
        return "Coupon deleted successfully!";
    }

    // ========= USER FUNCTIONS =========

    public List<Coupon> getActiveCoupons() throws ExecutionException, InterruptedException {
        List<Coupon> coupons = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db().collection(COLLECTION_NAME)
                .whereEqualTo("active", true)
                .get();
        for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
            coupons.add(doc.toObject(Coupon.class));
        }
        return coupons;
    }

    public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) throws Exception {
        String code = request.getCode();
        String userId = request.getUserId();
        double orderTotal = request.getOrderTotal();
        var items = request.getItems();

        // 1. Fetch coupon by code
        CollectionReference couponsRef = db().collection(COLLECTION_NAME);
        ApiFuture<QuerySnapshot> query = couponsRef.whereEqualTo("code", code).whereEqualTo("active", true).get();
        QuerySnapshot snapshot = query.get();

        if (snapshot.isEmpty()) {
            return new ApplyCouponResponse(false, 0, "Coupon not found or inactive");
        }

        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        Coupon coupon = doc.toObject(Coupon.class);

        if (coupon == null) {
            return new ApplyCouponResponse(false, 0, "Invalid coupon data");
        }

        // 2. Base discount (your existing logic)
        Double discountValue = coupon.getDiscount();
        double baseDiscount = (discountValue != null) ? discountValue : 0.0;

        // 🔹 3. PRODUCT-SPECIFIC LOGIC (if productId is set)
        if (coupon.getProductId() != null && !coupon.getProductId().isEmpty()) {
            boolean found = false;
            double productTotal = 0;

            for (CartItemDto item : items) {
                if (coupon.getProductId().equals(item.getProductId())) {
                    found = true;
                    productTotal += item.getPrice() * item.getQuantity();
                }
            }

            if (!found) {
                return new ApplyCouponResponse(false, 0,
                        "Coupon is valid only for a specific product in your cart");
            }

            // apply discount only on that product's total
            double discount = Math.min(baseDiscount, productTotal);
            return new ApplyCouponResponse(true, discount, "Product-specific coupon applied");
        }

        // 🔹 4. ORDER-COUNT LOGIC (if nthOrder is set)
        if (coupon.getNthOrder() != null && coupon.getNthOrder() > 0) {
            int orderCount = orderService.getOrderCountForUser(userId); // implement this
            int currentOrderNumber = orderCount + 1;

            if (currentOrderNumber != coupon.getNthOrder()) {
                return new ApplyCouponResponse(false, 0,
                        "Coupon is valid only on your " + coupon.getNthOrder() + "th order");
            }

            double discount = Math.min(baseDiscount, orderTotal);
            return new ApplyCouponResponse(true, discount, "Nth-order coupon applied");
        }

        // 🔹 5. DEFAULT: normal order-wide coupon
        double discount = Math.min(baseDiscount, orderTotal);
        return new ApplyCouponResponse(true, discount, "Coupon applied");
    }
}
