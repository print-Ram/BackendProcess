package com.choco.home.pojo;

import com.google.cloud.firestore.annotation.DocumentId;

public class Product {

    @DocumentId
    private String product_id;
    private String name;
    private String name_lower;
    private String description;
    private String imageurl;
    private String category;
	private int price;
	private String categoryDescription;   // optional
    private String categoryBgImageUrl;    // optional

    public Product() {
        
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageurl() {
        return imageurl;
    }

    public void setImageurl(String imageurl) {
        this.imageurl = imageurl;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

	public String getName_lower() {
		return name_lower;
	}

	public void setName_lower(String name_lower) {
		this.name_lower = name_lower;
	}
	
    
    public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategoryDescription() {
		return categoryDescription;
	}

	public void setCategoryDescription(String categoryDescription) {
		this.categoryDescription = categoryDescription;
	}

	public String getCategoryBgImageUrl() {
		return categoryBgImageUrl;
	}

	public void setCategoryBgImageUrl(String categoryBgImageUrl) {
		this.categoryBgImageUrl = categoryBgImageUrl;
	}
	
	
    
    
}
