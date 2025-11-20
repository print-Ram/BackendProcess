package com.choco.home.pojo; 

import java.util.Date;

public class Coupon {
    private String id;
    private String code;
    private double discount;
    private String description;
    private boolean active;
    private String productId;
    private Integer nthOrder;
    private Date createdAt;

    public Coupon() {}

    public Coupon(String id, String code, double discount, String description, boolean active, Date createdAt) {
        this.id = id;
        this.code = code;
        this.discount = discount;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    
    public String getId() { return id; }
    public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public Integer getNthOrder() {
		return nthOrder;
	}

	public void setNthOrder(Integer nthOrder) {
		this.nthOrder = nthOrder;
	}

	public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
