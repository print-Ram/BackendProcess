// ApplyCouponRequest.java
package com.choco.home.dto;

import java.util.List;

public class ApplyCouponRequest {
    private String code;
    private String userId;
    private Double orderTotal;
    private List<CartItemDto> items;

    // getters/setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getOrderTotal() { return orderTotal; }
    public void setOrderTotal(Double orderTotal) { this.orderTotal = orderTotal; }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
}
