package com.respiroc.gregfullstack.model;

import java.math.BigDecimal;

public class ProductVariant {
    private Long shopifyVariantId;
    private String title;
    private BigDecimal price;
    private String sku;
    private boolean available;

    public ProductVariant() {
    }

    public ProductVariant(Long shopifyVariantId, String title, BigDecimal price, String sku, boolean available) {
        this.shopifyVariantId = shopifyVariantId;
        this.title = title;
        this.price = price;
        this.sku = sku;
        this.available = available;
    }

    public Long getShopifyVariantId() {
        return shopifyVariantId;
    }

    public ProductVariant setShopifyVariantId(Long shopifyVariantId) {
        this.shopifyVariantId = shopifyVariantId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ProductVariant setTitle(String title) {
        this.title = title;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ProductVariant setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public ProductVariant setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public boolean isAvailable() {
        return available;
    }

    public ProductVariant setAvailable(boolean available) {
        this.available = available;
        return this;
    }
}
