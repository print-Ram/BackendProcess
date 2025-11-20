package com.choco.home.controller;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

import com.choco.home.dto.ApplyCouponRequest;
import com.choco.home.dto.ApplyCouponResponse;
import com.choco.home.pojo.Coupon;
import com.choco.home.service.CouponService;

@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "*")
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    // ========== ADMIN APIs ==========

    @PostMapping("/admin/create")
    public Coupon createCoupon(@RequestBody Coupon coupon) throws ExecutionException, InterruptedException {
        return service.createCoupon(coupon);
    }

    @GetMapping("/admin/all")
    public List<Coupon> getAllCoupons() throws ExecutionException, InterruptedException {
        return service.getAllCoupons();
    }

    @PutMapping("/admin/update/{id}")
    public Coupon updateCoupon(@PathVariable String id, @RequestBody Coupon coupon)
            throws ExecutionException, InterruptedException {
        return service.updateCoupon(id, coupon);
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(@RequestBody ApplyCouponRequest request) {
        try {
            ApplyCouponResponse response = service.applyCoupon(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApplyCouponResponse(false, 0, "Error applying coupon"));
        }
    }
    
    @DeleteMapping("/admin/delete/{id}")
    public String deleteCoupon(@PathVariable String id)
            throws ExecutionException, InterruptedException {
        return service.deleteCoupon(id);
    }

    // ========== USER APIs ==========

    @GetMapping("/active")
    public List<Coupon> getActiveCoupons() throws ExecutionException, InterruptedException {
        return service.getActiveCoupons();
    }

}
