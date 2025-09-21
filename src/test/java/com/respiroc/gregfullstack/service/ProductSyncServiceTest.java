package com.respiroc.gregfullstack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.respiroc.gregfullstack.model.Product;
import com.respiroc.gregfullstack.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSyncServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProductSyncService productSyncService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void syncProductsLimitsToFiftySavedRecords() throws Exception {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(buildProductsJson(55));
        when(productRepository.count()).thenReturn(0L, 50L);
        when(productRepository.findByShopifyProductId(anyLong())).thenReturn(Optional.empty());
        when(productRepository.pruneExcess(anyInt())).thenReturn(0);

        productSyncService.syncProducts();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(50)).save(productCaptor.capture());

        List<Product> savedProducts = productCaptor.getAllValues();
        assertEquals(50, savedProducts.size());
        savedProducts.forEach(product -> assertFalse(product.getVariants().isEmpty(), "variants should be captured"));

        verify(productRepository).pruneExcess(50);
    }

    private String buildProductsJson(int totalProducts) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode products = root.putArray("products");

        for (int i = 0; i < totalProducts; i++) {
            ObjectNode product = products.addObject();
            product.put("id", 1_000L + i);
            product.put("title", "Product " + i);
            product.put("handle", "product-" + i);
            product.put("product_type", "Activewear");

            ArrayNode variants = product.putArray("variants");
            ObjectNode variant = variants.addObject();
            variant.put("id", 10_000L + i);
            variant.put("title", "Variant " + i);
            variant.put("price", String.valueOf(299 + i));
            variant.put("sku", "SKU-" + i);
            variant.put("available", true);
        }

        return mapper.writeValueAsString(root);
    }
}
