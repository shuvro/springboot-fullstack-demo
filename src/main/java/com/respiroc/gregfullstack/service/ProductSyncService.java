package com.respiroc.gregfullstack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.respiroc.gregfullstack.model.Product;
import com.respiroc.gregfullstack.model.ProductVariant;
import com.respiroc.gregfullstack.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProductSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ProductSyncService.class);
    private static final String FAMME_PRODUCTS_URL = "https://famme.no/products.json";
    private static final int MAX_PRODUCTS = 50;

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ProductSyncService(ProductRepository productRepository, RestTemplate restTemplate) {
        this.productRepository = productRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(initialDelay = 0, fixedDelay = 3600000) // Run immediately, then every hour
    public void syncProducts() {
        logger.info("Starting product sync from Famme API...");
        
        try {
            String response = restTemplate.getForObject(FAMME_PRODUCTS_URL, String.class);
            if (response == null) {
                logger.error("Received null response from Famme API");
                return;
            }

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode productsNode = rootNode.get("products");
            
            if (productsNode == null || !productsNode.isArray()) {
                logger.error("Invalid response format - products array not found");
                return;
            }

            long initialCount = productRepository.count();
            long availableSlots = Math.max(0, MAX_PRODUCTS - initialCount);

            int processedCount = 0;
            int insertedCount = 0;
            int updatedCount = 0;
            int skippedInvalid = 0;
            int skippedByLimit = 0;

            for (JsonNode productNode : productsNode) {
                if (processedCount >= MAX_PRODUCTS) {
                    break;
                }

                processedCount++;

                try {
                    Product product = parseProduct(productNode);
                    if (product == null) {
                        skippedInvalid++;
                        continue;
                    }

                    Optional<Product> existingProduct = productRepository.findByShopifyProductId(product.getShopifyProductId());
                    if (existingProduct.isPresent()) {
                        Product current = existingProduct.get();
                        product.setId(current.getId());
                        product.setCreatedAt(current.getCreatedAt());
                        productRepository.save(product);
                        updatedCount++;
                    } else if (availableSlots > 0) {
                        productRepository.save(product);
                        availableSlots--;
                        insertedCount++;
                    } else {
                        skippedByLimit++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to process product node: {}", e.getMessage());
                    skippedInvalid++;
                }
            }

            int removed = productRepository.pruneExcess(MAX_PRODUCTS);
            long finalCount = productRepository.count();

            logger.info("Product sync completed. Processed: {}, Inserted: {}, Updated: {}, Skipped (invalid): {}, Skipped (limit): {}, Deleted excess: {}, Total in DB: {}",
                    processedCount, insertedCount, updatedCount, skippedInvalid, skippedByLimit, removed, finalCount);

        } catch (Exception e) {
            logger.error("Error during product sync: {}", e.getMessage(), e);
        }
    }

    private Product parseProduct(JsonNode productNode) {
        try {
            Long shopifyProductId = productNode.get("id").asLong();
            String title = productNode.get("title").asText();
            String handle = productNode.get("handle").asText();
            String productType = productNode.has("product_type") ? productNode.get("product_type").asText() : null;

            // Validate required fields
            if (title == null || title.trim().isEmpty() || handle == null || handle.trim().isEmpty()) {
                logger.warn("Skipping product with missing title or handle: {}", shopifyProductId);
                return null;
            }

            List<ProductVariant> variants = extractVariants(productNode.get("variants"));
            BigDecimal minPrice = variants.stream()
                    .map(ProductVariant::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            return new Product(shopifyProductId, title, handle, minPrice, productType, variants);

        } catch (Exception e) {
            logger.error("Error parsing product: {}", e.getMessage());
            return null;
        }
    }

    private List<ProductVariant> extractVariants(JsonNode variantsNode) {
        List<ProductVariant> variants = new ArrayList<>();

        if (variantsNode == null || !variantsNode.isArray()) {
            return variants;
        }

        for (JsonNode variantNode : variantsNode) {
            try {
                Long variantId = variantNode.has("id") ? variantNode.get("id").asLong() : null;
                String title = variantNode.has("title") ? variantNode.get("title").asText() : null;
                String sku = variantNode.has("sku") ? variantNode.get("sku").asText(null) : null;
                boolean available = variantNode.has("available") && variantNode.get("available").asBoolean();

                BigDecimal price = BigDecimal.ZERO;
                if (variantNode.has("price")) {
                    String priceStr = variantNode.get("price").asText();
                    try {
                        price = new BigDecimal(priceStr);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid price format for variant {}: {}", variantId, priceStr);
                    }
                }

                variants.add(new ProductVariant(variantId, title, price, sku, available));
            } catch (Exception variantError) {
                logger.debug("Skipping malformed variant: {}", variantError.getMessage());
            }
        }

        return variants;
    }

    public void forceSyncProducts() {
        logger.info("Manual product sync triggered");
        syncProducts();
    }
}
