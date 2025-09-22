package com.respiroc.gregfullstack.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.respiroc.gregfullstack.model.Product
import com.respiroc.gregfullstack.model.ProductVariant
import com.respiroc.gregfullstack.repository.ProductRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.util.Optional
import kotlin.math.max

@Service
class ProductSyncService(
    private val productRepository: ProductRepository,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {

    @Scheduled(initialDelay = 0, fixedDelay = 3600000) // Run immediately, then every hour
    fun syncProducts() {
        logger.info("Starting product sync from Famme API...")

        try {
            val response = restTemplate.getForObject(FAMME_PRODUCTS_URL, String::class.java)
            if (response == null) {
                logger.error("Received null response from Famme API")
                return
            }

            val rootNode = objectMapper.readTree(response)
            val productsNode = rootNode.get("products")

            if (productsNode == null || !productsNode.isArray) {
                logger.error("Invalid response format - products array not found")
                return
            }

            val initialCount = productRepository.count()
            var availableSlots = max(0L, MAX_PRODUCTS.toLong() - initialCount)

            var processedCount = 0
            var insertedCount = 0
            var updatedCount = 0
            var skippedInvalid = 0
            var skippedByLimit = 0

            for (productNode in productsNode) {
                if (processedCount >= MAX_PRODUCTS) {
                    break
                }

                processedCount++

                try {
                    val product = parseProduct(productNode)
                    if (product == null) {
                        skippedInvalid++
                        continue
                    }

                    val existingProduct: Optional<Product> =
                        productRepository.findByShopifyProductId(product.shopifyProductId)
                    if (existingProduct.isPresent) {
                        val current = existingProduct.get()
                        product.id = current.id
                        product.createdAt = current.createdAt
                        productRepository.save(product)
                        updatedCount++
                    } else if (availableSlots > 0) {
                        productRepository.save(product)
                        availableSlots--
                        insertedCount++
                    } else {
                        skippedByLimit++
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to process product node: {}", e.message)
                    skippedInvalid++
                }
            }

            val removed = productRepository.pruneExcess(MAX_PRODUCTS)
            val finalCount = productRepository.count()

            logger.info(
                "Product sync completed. Processed: {}, Inserted: {}, Updated: {}, Skipped (invalid): {}, Skipped (limit): {}, Deleted excess: {}, Total in DB: {}",
                processedCount, insertedCount, updatedCount, skippedInvalid, skippedByLimit, removed, finalCount
            )
        } catch (e: Exception) {
            logger.error("Error during product sync: {}", e.message, e)
        }
    }

    private fun parseProduct(productNode: JsonNode): Product? {
        return try {
            val shopifyProductId = productNode["id"]?.asLong()
            val title = productNode["title"]?.asText()?.trim()
            val handle = productNode["handle"]?.asText()?.trim()
            val productType = productNode.takeIf { it.has("product_type") }?.get("product_type")?.asText()

            if (shopifyProductId == null || title.isNullOrBlank() || handle.isNullOrBlank()) {
                logger.warn("Skipping product with missing title or handle: {}", shopifyProductId)
                return null
            }

            val variants = extractVariants(productNode.get("variants"))
            val minPrice = variants
                .mapNotNull(ProductVariant::price)
                .minOrNull() ?: BigDecimal.ZERO

            Product(shopifyProductId, title, handle, minPrice, productType, variants)
        } catch (e: Exception) {
            logger.error("Error parsing product: {}", e.message)
            null
        }
    }

    private fun extractVariants(variantsNode: JsonNode?): MutableList<ProductVariant> {
        val variants = mutableListOf<ProductVariant>()

        if (variantsNode == null || !variantsNode.isArray) {
            return variants
        }

        for (variantNode in variantsNode) {
            try {
                val variantId = variantNode.takeIf { it.has("id") }?.get("id")?.asLong()
                val title = variantNode.takeIf { it.has("title") }?.get("title")?.asText()
                val sku = variantNode.takeIf { it.has("sku") }?.get("sku")?.asText(null)
                val available = variantNode.has("available") && variantNode.get("available").asBoolean()

                var price = BigDecimal.ZERO
                if (variantNode.has("price")) {
                    val priceStr = variantNode.get("price").asText()
                    try {
                        price = BigDecimal(priceStr)
                    } catch (e: NumberFormatException) {
                        logger.warn("Invalid price format for variant {}: {}", variantId, priceStr)
                    }
                }

                variants.add(ProductVariant(variantId, title, price, sku, available))
            } catch (variantError: Exception) {
                logger.debug("Skipping malformed variant: {}", variantError.message)
            }
        }

        return variants
    }

    fun forceSyncProducts() {
        logger.info("Manual product sync triggered")
        syncProducts()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProductSyncService::class.java)
        private const val FAMME_PRODUCTS_URL = "https://famme.no/products.json"
        private const val MAX_PRODUCTS = 50
    }
}
