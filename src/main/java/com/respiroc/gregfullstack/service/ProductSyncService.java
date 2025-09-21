package com.respiroc.gregfullstack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.respiroc.gregfullstack.model.Product;
import com.respiroc.gregfullstack.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

            List<Product> productsToSave = new ArrayList<>();
            int processedCount = 0;

            for (JsonNode productNode : productsNode) {
                if (processedCount >= MAX_PRODUCTS) {
                    logger.info("Reached maximum limit of {} products", MAX_PRODUCTS);
                    break;
                }

                try {
                    Product product = parseProduct(productNode);
                    if (product != null) {
                        // Check if product already exists
                        if (productRepository.findByShopifyProductId(product.getShopifyProductId()).isEmpty()) {
                            productsToSave.add(product);
                            processedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse product: {}", e.getMessage());
                }
            }

            // Save new products
            int savedCount = 0;
            for (Product product : productsToSave) {
                try {
                    productRepository.save(product);
                    savedCount++;
                } catch (Exception e) {
                    logger.error("Failed to save product {}: {}", product.getTitle(), e.getMessage());
                }
            }

            logger.info("Product sync completed. Processed: {}, Saved: {}, Total in DB: {}", 
                       processedCount, savedCount, productRepository.count());

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

            // Get price from the first variant
            BigDecimal price = BigDecimal.ZERO;
            JsonNode variantsNode = productNode.get("variants");
            if (variantsNode != null && variantsNode.isArray() && variantsNode.size() > 0) {
                JsonNode firstVariant = variantsNode.get(0);
                if (firstVariant.has("price")) {
                    String priceStr = firstVariant.get("price").asText();
                    try {
                        price = new BigDecimal(priceStr);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid price format for product {}: {}", title, priceStr);
                        price = BigDecimal.ZERO;
                    }
                }
            }

            // Validate required fields
            if (title == null || title.trim().isEmpty() || handle == null || handle.trim().isEmpty()) {
                logger.warn("Skipping product with missing title or handle: {}", shopifyProductId);
                return null;
            }

            return new Product(shopifyProductId, title, handle, price, productType);

        } catch (Exception e) {
            logger.error("Error parsing product: {}", e.getMessage());
            return null;
        }
    }

    public void forceSyncProducts() {
        logger.info("Manual product sync triggered");
        syncProducts();
    }
}
