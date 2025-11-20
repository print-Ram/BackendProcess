// ApplyCouponResponse.java
package com.choco.home.dto;

public class ApplyCouponResponse {
    private boolean valid;
    private double discount;
    private String message;

    public ApplyCouponResponse() {}

    public ApplyCouponResponse(boolean valid, double discount, String message) {
        this.valid = valid;
        this.discount = discount;
        this.message = message;
    }

    // getters/setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
