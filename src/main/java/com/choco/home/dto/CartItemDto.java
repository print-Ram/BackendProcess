// CartItemDto.java
package com.choco.home.dto;

public class CartItemDto {
    private String productId;
    private Integer quantity;
    private Double price;

    // getters/setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
