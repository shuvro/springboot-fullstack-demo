package com.respiroc.gregfullstack.model

import java.math.BigDecimal
import java.time.LocalDateTime

class Product @JvmOverloads constructor(
    var shopifyProductId: Long? = null,
    var title: String? = null,
    var handle: String? = null,
    var price: BigDecimal? = null,
    var productType: String? = null,
    var variants: MutableList<ProductVariant> = mutableListOf()
) {
    var id: Long? = null
    var createdAt: LocalDateTime? = null
    var updatedAt: LocalDateTime? = null

    override fun toString(): String {
        return "Product{" +
            "id=" + id +
            ", shopifyProductId=" + shopifyProductId +
            ", title='" + title + '\'' +
            ", handle='" + handle + '\'' +
            ", price=" + price +
            ", productType='" + productType + '\'' +
            ", variantsCount=" + variants.size +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}'
    }
}
