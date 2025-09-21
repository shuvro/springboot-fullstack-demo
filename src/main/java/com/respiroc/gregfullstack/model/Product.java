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

    public Product() {}

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

    public Product setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getShopifyProductId() {
        return shopifyProductId;
    }

    public Product setShopifyProductId(Long shopifyProductId) {
        this.shopifyProductId = shopifyProductId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Product setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getHandle() {
        return handle;
    }

    public Product setHandle(String handle) {
        this.handle = handle;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Product setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getProductType() {
        return productType;
    }

    public Product setProductType(String productType) {
        this.productType = productType;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Product setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Product setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public Product setVariants(List<ProductVariant> variants) {
        this.variants = variants != null ? new ArrayList<>(variants) : new ArrayList<>();
        return this;
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
