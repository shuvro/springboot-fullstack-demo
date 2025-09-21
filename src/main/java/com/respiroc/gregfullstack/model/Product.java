package com.respiroc.gregfullstack.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Product {
    private Long id;
    private Long shopifyProductId;
    private String title;
    private String handle;
    private BigDecimal price;
    private String productType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductVariant> variants = new ArrayList<>();

    // Default constructor
    public Product() {}

    // Constructor for creating new products
    public Product(Long shopifyProductId, String title, String handle, BigDecimal price, String productType) {
        this(shopifyProductId, title, handle, price, productType, Collections.emptyList());
    }

    public Product(Long shopifyProductId,
                   String title,
                   String handle,
                   BigDecimal price,
                   String productType,
                   List<ProductVariant> variants) {
        this.shopifyProductId = shopifyProductId;
        this.title = title;
        this.handle = handle;
        this.price = price;
        this.productType = productType;
        if (variants != null) {
            this.variants = new ArrayList<>(variants);
        }
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

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants != null ? new ArrayList<>(variants) : new ArrayList<>();
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
                ", variantsCount=" + (variants != null ? variants.size() : 0) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
