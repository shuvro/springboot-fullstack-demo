package com.respiroc.gregfullstack.repository

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.respiroc.gregfullstack.model.Product
import com.respiroc.gregfullstack.model.ProductVariant
import org.springframework.jdbc.core.RowMapper
import org.postgresql.util.PGobject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.Optional
import kotlin.math.max

@Repository
class ProductRepository(private val jdbcClient: JdbcClient, private val objectMapper: ObjectMapper) {

    private val productRowMapper = RowMapper<Product> { rs: ResultSet, _ -> mapProduct(rs) }

    fun findAll(): List<Product> {
        val sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            ORDER BY created_at DESC
            
            """.trimIndent()

        return jdbcClient.sql(sql)
            .query(productRowMapper)
            .list()
    }

    fun findPage(offset: Int, limit: Int): List<Product> {
        if (limit <= 0) {
            return emptyList()
        }

        val sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            
            """.trimIndent()

        val safeOffset = max(offset, 0)

        return jdbcClient.sql(sql)
            .param(limit)
            .param(safeOffset)
            .query(productRowMapper)
            .list()
    }

    fun searchByTitle(query: String): List<Product> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }

        val pattern = "%" + escapeForLike(trimmed) + "%"

        val sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            WHERE title ILIKE ? ESCAPE '\'
            ORDER BY created_at DESC
            
            """.trimIndent()

        return jdbcClient.sql(sql)
            .param(pattern)
            .query(productRowMapper)
            .list()
    }

    fun findById(id: Long?): Optional<Product> {
        val sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            WHERE id = ?
            
            """.trimIndent()

        return jdbcClient.sql(sql)
            .param(id)
            .query(productRowMapper)
            .optional()
    }

    fun findByShopifyProductId(shopifyProductId: Long?): Optional<Product> {
        val sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            WHERE shopify_product_id = ?
            
            """.trimIndent()

        return jdbcClient.sql(sql)
            .param(shopifyProductId)
            .query(productRowMapper)
            .optional()
    }

    fun save(product: Product): Product {
        if (product.id == null) {
            return insert(product)
        } else {
            return update(product)
        }
    }

    private fun insert(product: Product): Product {
        val sql = """
            INSERT INTO products (shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            
            """.trimIndent()

        val keyHolder: KeyHolder = GeneratedKeyHolder()
        val now = LocalDateTime.now()

        jdbcClient.sql(sql)
            .param(product.shopifyProductId)
            .param(product.title)
            .param(product.handle)
            .param(product.price)
            .param(product.productType)
            .param(writeVariants(product.variants))
            .param(Timestamp.valueOf(now))
            .param(Timestamp.valueOf(now))
            .update(keyHolder, "id")

        val keys = keyHolder.keys
        if (keys != null && keys.containsKey("id")) {
            val idValue = keys["id"]
            if (idValue is Number) {
                product.id = idValue.toLong()
            }
        }

        product.createdAt = now
        product.updatedAt = now

        return product
    }

    private fun update(product: Product): Product {
        val sql = """
            UPDATE products 
            SET shopify_product_id = ?, title = ?, handle = ?, price = ?, product_type = ?, variants = ?, updated_at = ?
            WHERE id = ?
            
            """.trimIndent()

        val now = LocalDateTime.now()

        jdbcClient.sql(sql)
            .param(product.shopifyProductId)
            .param(product.title)
            .param(product.handle)
            .param(product.price)
            .param(product.productType)
            .param(writeVariants(product.variants))
            .param(Timestamp.valueOf(now))
            .param(product.id)
            .update()

        product.updatedAt = now
        return product
    }

    fun deleteById(id: Long?) {
        val sql = "DELETE FROM products WHERE id = ?"
        jdbcClient.sql(sql).param(id).update()
    }

    fun count(): Long {
        val sql = "SELECT COUNT(*) FROM products"
        return jdbcClient.sql(sql).query(Long::class.java).single()
    }

    fun pruneExcess(limit: Int): Int {
        val sql = """
            DELETE FROM products
            WHERE id IN (
                SELECT id FROM products
                ORDER BY updated_at DESC, id DESC
                OFFSET ?
            )
            
            """.trimIndent()

        return jdbcClient.sql(sql)
            .param(limit)
            .update()
    }

    private fun readVariants(value: Any?): MutableList<ProductVariant> {
        if (value == null) {
            return mutableListOf()
        }

        val json = if (value is PGobject) value.value else value.toString()

        if (json.isNullOrBlank()) {
            return mutableListOf()
        }

        try {
            val variants = objectMapper.readValue(json, VARIANT_LIST_TYPE)
            return variants ?: mutableListOf()
        } catch (e: JsonProcessingException) {
            logger.warn("Failed to parse variants JSON: {}", e.message)
            return mutableListOf()
        }
    }

    private fun writeVariants(variants: MutableList<ProductVariant>?): PGobject {
        try {
            val jsonObject = PGobject()
            jsonObject.type = "jsonb"
            val safeVariants = variants ?: mutableListOf<ProductVariant>()
            jsonObject.value = objectMapper.writeValueAsString(safeVariants)
            return jsonObject
        } catch (e: SQLException) {
            throw IllegalStateException("Failed to serialise product variants", e)
        } catch (e: JsonProcessingException) {
            throw IllegalStateException("Failed to serialise product variants", e)
        }
    }

    private fun escapeForLike(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    private fun mapProduct(rs: ResultSet): Product {
        val product = Product()
        product.id = rs.getLong("id")
        if (rs.wasNull()) {
            product.id = null
        }

        val shopifyId = rs.getObject("shopify_product_id") as? Number
        product.shopifyProductId = shopifyId?.toLong()
        product.title = rs.getString("title")
        product.handle = rs.getString("handle")
        product.price = rs.getBigDecimal("price")
        product.productType = rs.getString("product_type")
        product.variants = readVariants(rs.getObject("variants"))

        val createdAt = rs.getTimestamp("created_at")
        val updatedAt = rs.getTimestamp("updated_at")
        product.createdAt = createdAt?.toLocalDateTime()
        product.updatedAt = updatedAt?.toLocalDateTime()

        return product
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProductRepository::class.java)
        private val VARIANT_LIST_TYPE: TypeReference<MutableList<ProductVariant>?> =
            object : TypeReference<MutableList<ProductVariant>?>() {}
    }
}
