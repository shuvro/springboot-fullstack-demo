package com.respiroc.gregfullstack.controller

import com.respiroc.gregfullstack.model.Product
import com.respiroc.gregfullstack.model.ProductVariant
import com.respiroc.gregfullstack.repository.ProductRepository
import com.respiroc.gregfullstack.service.ProductSyncService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.max

@Controller
class ProductController(
    private val productRepository: ProductRepository,
    private val productSyncService: ProductSyncService
) {
    @GetMapping("/")
    fun index(model: Model): String {
        logger.info("Accessing home page")
        model.addAttribute("productCount", productRepository.count())
        return "index"
    }

    @GetMapping("/search")
    fun searchPage(model: Model): String {
        logger.info("Accessing product search page")
        model.addAttribute("totalProducts", productRepository.count())
        return "search"
    }

    @GetMapping("/search/results")
    fun searchProducts(@RequestParam(name = "q", required = false) query: String?, model: Model): String {
        val searchTerm = query?.trim().orEmpty()
        val searchPerformed = searchTerm.isNotEmpty()

        logger.info("Searching for products with query: '{}'", searchTerm)

        val products = if (searchPerformed) productRepository.searchByTitle(searchTerm) else emptyList<Product>()

        model.addAttribute("products", products)
        model.addAttribute("searchTerm", query.orEmpty())
        model.addAttribute("searchPerformed", searchPerformed)
        model.addAttribute("matchCount", products.size)

        return "fragments/product-search-rows"
    }

    @GetMapping("/products")
    fun loadProducts(
        @RequestParam(value = "page", required = false) page: Int?,
        @RequestParam(value = "size", required = false) size: Int?,
        model: Model
    ): String {
        logger.info("Loading products via HTMX: page={}, size={}", page, size)
        return renderProductPage(page, size, model)
    }

    private fun renderProductPage(requestedPage: Int?, requestedSize: Int?, model: Model): String {
        var pageSize = requestedSize ?: DEFAULT_PAGE_SIZE
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE
        }

        var pageNumber = requestedPage ?: 0
        if (pageNumber < 0) {
            pageNumber = 0
        }

        val totalProducts = productRepository.count()
        val totalPages = if (totalProducts == 0L) 0 else ceil(totalProducts.toDouble() / pageSize.toDouble()).toInt()

        if (totalPages in 1..pageNumber) {
            pageNumber = totalPages - 1
        } else if (totalPages == 0) {
            pageNumber = 0
        }

        val offset = pageNumber * pageSize
        val products = if (totalProducts == 0L) emptyList<Product>() else productRepository.findPage(offset, pageSize)

        val pageStart = if (totalProducts == 0L) 0 else offset + 1L
        val pageEnd: Long
        if (totalProducts == 0L || products.isEmpty()) {
            pageEnd = if (totalProducts == 0L) 0 else max(pageStart - 1L, 0L)
        } else {
            pageEnd = pageStart + products.size - 1L
        }

        model.addAttribute("products", products)
        model.addAttribute("productCount", totalProducts)
        model.addAttribute("currentPage", pageNumber)
        model.addAttribute("pageSize", pageSize)
        model.addAttribute("totalPages", totalPages)
        model.addAttribute("totalProducts", totalProducts)
        model.addAttribute("hasPrevious", pageNumber > 0)
        model.addAttribute("hasNext", totalPages > 0 && pageNumber + 1 < totalPages)
        model.addAttribute("pageStart", pageStart)
        model.addAttribute("pageEnd", pageEnd)

        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", null)
        }
        return "fragments/product-rows"
    }

    @GetMapping("/products/{id}")
    fun viewProduct(@PathVariable id: Long?, model: Model): String {
        val product = productRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found") }

        logger.info("Viewing product detail for id={}", id)

        model.addAttribute("product", product)
        model.addAttribute("productCount", productRepository.count())
        return "product-detail"
    }

    @PostMapping("/products/sync")
    fun syncProducts(model: Model): String {
        logger.info("Manual product sync triggered via HTMX")
        try {
            productSyncService.forceSyncProducts()
            val count = productRepository.count()
            model.addAttribute("success", true)
            model.addAttribute("message", "Products synced successfully! Total products: " + count)
        } catch (e: Exception) {
            logger.error("Error during manual sync: {}", e.message, e)
            model.addAttribute("success", false)
            model.addAttribute("message", "Error syncing products: " + e.message)
        }
        return "fragments/sync-status"
    }

    @PostMapping("/products")
    fun addProduct(
        @RequestParam title: String?,
        @RequestParam handle: String?,
        @RequestParam price: BigDecimal?,
        @RequestParam(required = false) productType: String?,
        model: Model
    ): String {
        logger.info("Adding new product: {}", title)

        try {
            val variant = ProductVariant(null, title, price, null, true)
            val product = Product(null, title, handle, price, productType, mutableListOf(variant))
            productRepository.save(product)
            model.addAttribute("errorMessage", null)
            return renderProductPage(0, DEFAULT_PAGE_SIZE, model)
        } catch (e: Exception) {
            logger.error("Error adding product: {}", e.message, e)
            model.addAttribute("errorMessage", e.message)
            return renderProductPage(0, DEFAULT_PAGE_SIZE, model)
        }
    }

    @GetMapping("/products/form")
    fun showProductForm(): String {
        return "fragments/product-form"
    }

    @GetMapping("/products/form-close")
    fun closeProductForm(): String {
        return "fragments/empty"
    }

    @PostMapping("/products/{id}")
    fun updateProduct(
        @PathVariable id: Long?,
        @RequestParam title: String,
        @RequestParam handle: String,
        @RequestParam price: BigDecimal,
        @RequestParam(required = false) productType: String?,
        @RequestParam(value = "shopifyProductId", required = false) shopifyProductIdValue: String?,
        redirectAttributes: RedirectAttributes
    ): String {
        val product = productRepository.findById(id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")
            }

        logger.info("Updating product id={} with new details", id)

        var shopifyProductId: Long? = null
        if (shopifyProductIdValue != null && !shopifyProductIdValue.isBlank()) {
            try {
                shopifyProductId = shopifyProductIdValue.trim().toLong()
            } catch (ex: NumberFormatException) {
                redirectAttributes.addFlashAttribute("updateError", "Shopify product ID must be a number.")
                return "redirect:/products/$id"
            }
        }

        if (price < BigDecimal.ZERO) {
            redirectAttributes.addFlashAttribute("updateError", "Price cannot be negative.")
            return "redirect:/products/$id"
        }

        product.shopifyProductId = shopifyProductId
        product.title = title.trim()
        product.handle = handle.trim()
        product.price = price
        product.productType = productType?.takeIf { it.isNotBlank() }?.trim()

        productRepository.save(product)

        redirectAttributes.addFlashAttribute("updateSuccess", true)
        return "redirect:/products/$id"
    }

    @PostMapping("/products/{id}/delete")
    fun deleteProduct(
        @PathVariable id: Long?,
        @RequestParam(value = "q", required = false) query: String?,
        @RequestParam(value = "page", required = false) page: Int?,
        @RequestParam(value = "size", required = false) size: Int?,
        @RequestHeader(value = "HX-Request", required = false) isHxRequest: Boolean?,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val product = productRepository.findById(id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")
            }

        productRepository.deleteById(id)
        logger.info("Deleted product id={} (title={})", id, product.title)

        val hxRequest = isHxRequest == true

        if (hxRequest) {
            if (query != null) {
                return searchProducts(query, model)
            }
            return renderProductPage(page, size, model)
        }

        redirectAttributes.addFlashAttribute("productDeleted", true)
        return "redirect:/"
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProductController::class.java)
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 50
    }
}
