package com.respiroc.gregfullstack.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.respiroc.gregfullstack.model.Product;
import com.respiroc.gregfullstack.model.ProductVariant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Repository
public class ProductRepository {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);
    private static final TypeReference<List<ProductVariant>> VARIANT_LIST_TYPE = new TypeReference<>() {};

    public ProductRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public List<Product> findAll() {
        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            ORDER BY created_at DESC
            """;
        
        return jdbcClient.sql(sql)
                .query(this::mapProduct)
                .list();
    }

    public List<Product> findPage(int offset, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;

        int safeOffset = Math.max(offset, 0);

        return jdbcClient.sql(sql)
                .param(limit)
                .param(safeOffset)
                .query(this::mapProduct)
                .list();
    }

    public List<Product> searchByTitle(String query) {
        if (query == null) {
            return List.of();
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        String pattern = "%" + escapeForLike(trimmed) + "%";

        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            WHERE title ILIKE ? ESCAPE '\\'
            ORDER BY created_at DESC
            """;

        return jdbcClient.sql(sql)
                .param(pattern)
                .query(this::mapProduct)
                .list();
    }

    public Optional<Product> findById(Long id) {
        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            WHERE id = ?
            """;
        
        return jdbcClient.sql(sql)
                .param(id)
                .query(this::mapProduct)
                .optional();
    }

    public Optional<Product> findByShopifyProductId(Long shopifyProductId) {
        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at
            FROM products
            WHERE shopify_product_id = ?
            """;
        
        return jdbcClient.sql(sql)
                .param(shopifyProductId)
                .query(this::mapProduct)
                .optional();
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            return insert(product);
        } else {
            return update(product);
        }
    }

    private Product insert(Product product) {
        String sql = """
            INSERT INTO products (shopify_product_id, title, handle, price, product_type, variants, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        
        jdbcClient.sql(sql)
                .param(product.getShopifyProductId())
                .param(product.getTitle())
                .param(product.getHandle())
                .param(product.getPrice())
                .param(product.getProductType())
                .param(writeVariants(product.getVariants()))
                .param(Timestamp.valueOf(now))
                .param(Timestamp.valueOf(now))
                .update(keyHolder, "id");

        // Get the generated ID from the keyholder
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("id")) {
            Object idValue = keys.get("id");
            if (idValue instanceof Number idNumber) {
                product.setId(idNumber.longValue());
            }
        }

        return product
                .setCreatedAt(now)
                .setUpdatedAt(now);
    }

    private Product update(Product product) {
        String sql = """
            UPDATE products 
            SET shopify_product_id = ?, title = ?, handle = ?, price = ?, product_type = ?, variants = ?, updated_at = ?
            WHERE id = ?
            """;
        
        LocalDateTime now = LocalDateTime.now();
        
        jdbcClient.sql(sql)
                .param(product.getShopifyProductId())
                .param(product.getTitle())
                .param(product.getHandle())
                .param(product.getPrice())
                .param(product.getProductType())
                .param(writeVariants(product.getVariants()))
                .param(Timestamp.valueOf(now))
                .param(product.getId())
                .update();

        return product.setUpdatedAt(now);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        jdbcClient.sql(sql).param(id).update();
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM products";
        return jdbcClient.sql(sql).query(Long.class).single();
    }

    public int pruneExcess(int limit) {
        String sql = """
            DELETE FROM products
            WHERE id IN (
                SELECT id FROM products
                ORDER BY updated_at DESC, id DESC
                OFFSET ?
            )
            """;

        return jdbcClient.sql(sql)
                .param(limit)
                .update();
    }

    private List<ProductVariant> readVariants(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        String json;
        if (value instanceof PGobject pgObject) {
            json = pgObject.getValue();
        } else {
            json = value.toString();
        }

        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<ProductVariant> variants = objectMapper.readValue(json, VARIANT_LIST_TYPE);
            return variants != null ? variants : new ArrayList<>();
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse variants JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private PGobject writeVariants(List<ProductVariant> variants) {
        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(objectMapper.writeValueAsString(variants != null ? variants : Collections.emptyList()));
            return jsonObject;
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise product variants", e);
        }
    }

    private String escapeForLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private Product mapProduct(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new Product()
                .setId(rs.getLong("id"))
                .setShopifyProductId(rs.getLong("shopify_product_id"))
                .setTitle(rs.getString("title"))
                .setHandle(rs.getString("handle"))
                .setPrice(rs.getBigDecimal("price"))
                .setProductType(rs.getString("product_type"))
                .setVariants(readVariants(rs.getObject("variants")))
                .setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                .setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
    }
}
