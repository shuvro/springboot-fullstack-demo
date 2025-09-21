package com.respiroc.gregfullstack.repository;

import com.respiroc.gregfullstack.model.Product;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcClient jdbcClient;

    public ProductRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Product> findAll() {
        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, created_at, updated_at
            FROM products
            ORDER BY created_at DESC
            """;
        
        return jdbcClient.sql(sql)
                .query((rs, rowNum) -> {
                    Product product = new Product();
                    product.setId(rs.getLong("id"));
                    product.setShopifyProductId(rs.getLong("shopify_product_id"));
                    product.setTitle(rs.getString("title"));
                    product.setHandle(rs.getString("handle"));
                    product.setPrice(rs.getBigDecimal("price"));
                    product.setProductType(rs.getString("product_type"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        product.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        product.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    return product;
                })
                .list();
    }

    public Optional<Product> findById(Long id) {
        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, created_at, updated_at
            FROM products
            WHERE id = ?
            """;
        
        return jdbcClient.sql(sql)
                .param(id)
                .query((rs, rowNum) -> {
                    Product product = new Product();
                    product.setId(rs.getLong("id"));
                    product.setShopifyProductId(rs.getLong("shopify_product_id"));
                    product.setTitle(rs.getString("title"));
                    product.setHandle(rs.getString("handle"));
                    product.setPrice(rs.getBigDecimal("price"));
                    product.setProductType(rs.getString("product_type"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        product.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        product.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    return product;
                })
                .optional();
    }

    public Optional<Product> findByShopifyProductId(Long shopifyProductId) {
        String sql = """
            SELECT id, shopify_product_id, title, handle, price, product_type, created_at, updated_at
            FROM products
            WHERE shopify_product_id = ?
            """;
        
        return jdbcClient.sql(sql)
                .param(shopifyProductId)
                .query((rs, rowNum) -> {
                    Product product = new Product();
                    product.setId(rs.getLong("id"));
                    product.setShopifyProductId(rs.getLong("shopify_product_id"));
                    product.setTitle(rs.getString("title"));
                    product.setHandle(rs.getString("handle"));
                    product.setPrice(rs.getBigDecimal("price"));
                    product.setProductType(rs.getString("product_type"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        product.setCreatedAt(createdAt.toLocalDateTime());
                    }
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        product.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    return product;
                })
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
            INSERT INTO products (shopify_product_id, title, handle, price, product_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        
        jdbcClient.sql(sql)
                .param(product.getShopifyProductId())
                .param(product.getTitle())
                .param(product.getHandle())
                .param(product.getPrice())
                .param(product.getProductType())
                .param(Timestamp.valueOf(now))
                .param(Timestamp.valueOf(now))
                .update(keyHolder);
        
        Number generatedId = keyHolder.getKey();
        if (generatedId != null) {
            product.setId(generatedId.longValue());
            product.setCreatedAt(now);
            product.setUpdatedAt(now);
        }
        
        return product;
    }

    private Product update(Product product) {
        String sql = """
            UPDATE products 
            SET shopify_product_id = ?, title = ?, handle = ?, price = ?, product_type = ?, updated_at = ?
            WHERE id = ?
            """;
        
        LocalDateTime now = LocalDateTime.now();
        
        jdbcClient.sql(sql)
                .param(product.getShopifyProductId())
                .param(product.getTitle())
                .param(product.getHandle())
                .param(product.getPrice())
                .param(product.getProductType())
                .param(Timestamp.valueOf(now))
                .param(product.getId())
                .update();
        
        product.setUpdatedAt(now);
        return product;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        jdbcClient.sql(sql).param(id).update();
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM products";
        return jdbcClient.sql(sql).query(Long.class).single();
    }
}
