package com.respiroc.gregfullstack.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
    private Long id;
    private Long shopifyProductId;
    private String title;
    private String handle;
    private BigDecimal price;
    private String productType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Product() {}

    // Constructor for creating new products
    public Product(Long shopifyProductId, String title, String handle, BigDecimal price, String productType) {
        this.shopifyProductId = shopifyProductId;
        this.title = title;
        this.handle = handle;
        this.price = price;
        this.productType = productType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShopifyProductId() {
        return shopifyProductId;
    }

    public void setShopifyProductId(Long shopifyProductId) {
        this.shopifyProductId = shopifyProductId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", shopifyProductId=" + shopifyProductId +
                ", title='" + title + '\'' +
                ", handle='" + handle + '\'' +
                ", price=" + price +
                ", productType='" + productType + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
