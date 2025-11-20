package com.choco.home.pojo;

import jakarta.persistence.Transient;

public class Item {
    private String product_id;
    private int quantity;
    private double price;
    
    @Transient
    private String product_name;

    // Add getter and setter
    public String getProduct_name() {
        return product_name;
    }
    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    // Getters and setters
  

    public int getQuantity() {
        return quantity;
    }

    public String getProduct_id() {
		return product_id;
	}

	public void setProduct_id(String product_id) {
		this.product_id = product_id;
	}

	public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
