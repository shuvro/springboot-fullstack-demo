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

    public void setShopifyVariantId(Long shopifyVariantId) {
        this.shopifyVariantId = shopifyVariantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
