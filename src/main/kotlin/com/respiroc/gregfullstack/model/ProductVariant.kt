package com.respiroc.gregfullstack.model

import java.math.BigDecimal

class ProductVariant @JvmOverloads constructor(
    var shopifyVariantId: Long? = null,
    var title: String? = null,
    var price: BigDecimal? = null,
    var sku: String? = null,
    var isAvailable: Boolean = false
) {
    override fun toString(): String {
        return "ProductVariant{" +
            "shopifyVariantId=" + shopifyVariantId +
            ", title='" + title + '\'' +
            ", price=" + price +
            ", sku='" + sku + '\'' +
            ", available=" + isAvailable +
            '}'
    }
}
