package com.respiroc.gregfullstack.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.respiroc.gregfullstack.model.Product
import com.respiroc.gregfullstack.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.client.RestTemplate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductSyncServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var productSyncService: ProductSyncService

    private val mapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        productSyncService = ProductSyncService(productRepository, restTemplate, ObjectMapper())
    }

    @Test
    fun syncProductsLimitsToFiftySavedRecords() {
        given(restTemplate.getForObject(anyString(), eq(String::class.java)))
            .willReturn(buildProductsJson(55))
        given(productRepository.count()).willReturn(0L, 50L)
        given(productRepository.findByShopifyProductId(anyLong())).willReturn(Optional.empty())
        given(productRepository.pruneExcess(anyInt())).willReturn(0)
        given(productRepository.save(org.mockito.ArgumentMatchers.any(Product::class.java))).willAnswer { invocation ->
            invocation.getArgument<Product>(0)
        }

        productSyncService.syncProducts()

        val productCaptor: ArgumentCaptor<Product> = ArgumentCaptor.forClass(Product::class.java)
        verify(productRepository, times(50)).save(productCaptor.capture())

        val savedProducts = productCaptor.allValues
        assertEquals(50, savedProducts.size)
        savedProducts.forEach { product ->
            assertTrue(product.variants.isNotEmpty(), "variants should be captured")
        }

        verify(productRepository).pruneExcess(50)
    }

    private fun buildProductsJson(totalProducts: Int): String {
        val root: ObjectNode = mapper.createObjectNode()
        val products: ArrayNode = root.putArray("products")

        repeat(totalProducts) { index ->
            val product = products.addObject()
            product.put("id", 1_000L + index)
            product.put("title", "Product $index")
            product.put("handle", "product-$index")
            product.put("product_type", "Activewear")

            val variants = product.putArray("variants")
            val variant = variants.addObject()
            variant.put("id", 10_000L + index)
            variant.put("title", "Variant $index")
            variant.put("price", (299 + index).toString())
            variant.put("sku", "SKU-$index")
            variant.put("available", true)
        }

        return mapper.writeValueAsString(root)
    }
}
