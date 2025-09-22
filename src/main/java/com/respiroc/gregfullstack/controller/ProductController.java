package com.respiroc.gregfullstack.controller;

import com.respiroc.gregfullstack.model.Product;
import com.respiroc.gregfullstack.model.ProductVariant;
import com.respiroc.gregfullstack.repository.ProductRepository;
import com.respiroc.gregfullstack.service.ProductSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final ProductSyncService productSyncService;

    public ProductController(ProductRepository productRepository, ProductSyncService productSyncService) {
        this.productRepository = productRepository;
        this.productSyncService = productSyncService;
    }

    @GetMapping("/")
    public String index(Model model) {
        logger.info("Accessing home page");
        model.addAttribute("productCount", productRepository.count());
        return "index";
    }

    @GetMapping("/search")
    public String searchPage(Model model) {
        logger.info("Accessing product search page");
        model.addAttribute("totalProducts", productRepository.count());
        return "search";
    }

    @GetMapping("/search/results")
    public String searchProducts(@RequestParam(name = "q", required = false) String query, Model model) {
        String searchTerm = query != null ? query.trim() : "";
        boolean searchPerformed = !searchTerm.isEmpty();

        logger.info("Searching for products with query: '{}'", searchTerm);

        List<Product> products = searchPerformed ? productRepository.searchByTitle(searchTerm) : List.of();

        model.addAttribute("products", products);
        model.addAttribute("searchTerm", query != null ? query : "");
        model.addAttribute("searchPerformed", searchPerformed);
        model.addAttribute("matchCount", products.size());

        return "fragments/product-search-rows";
    }

    @GetMapping("/products")
    public String loadProducts(@RequestParam(value = "page", required = false) Integer page,
                               @RequestParam(value = "size", required = false) Integer size,
                               Model model) {
        logger.info("Loading products via HTMX: page={}, size={}", page, size);
        return renderProductPage(page, size, model);
    }

    private String renderProductPage(Integer requestedPage, Integer requestedSize, Model model) {
        int pageSize = requestedSize != null ? requestedSize : DEFAULT_PAGE_SIZE;
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        int pageNumber = requestedPage != null ? requestedPage : 0;
        if (pageNumber < 0) {
            pageNumber = 0;
        }

        long totalProducts = productRepository.count();
        int totalPages = totalProducts == 0 ? 0 : (int) Math.ceil((double) totalProducts / pageSize);

        if (totalPages > 0 && pageNumber >= totalPages) {
            pageNumber = totalPages - 1;
        } else if (totalPages == 0) {
            pageNumber = 0;
        }

        int offset = pageNumber * pageSize;
        List<Product> products = totalProducts == 0 ? List.of() : productRepository.findPage(offset, pageSize);

        long pageStart = totalProducts == 0 ? 0 : offset + 1L;
        long pageEnd;
        if (totalProducts == 0 || products.isEmpty()) {
            pageEnd = totalProducts == 0 ? 0 : Math.max(pageStart - 1L, 0L);
        } else {
            pageEnd = pageStart + products.size() - 1L;
        }

        model.addAttribute("products", products);
        model.addAttribute("productCount", totalProducts);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("hasPrevious", pageNumber > 0);
        model.addAttribute("hasNext", totalPages > 0 && pageNumber + 1 < totalPages);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);

        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", null);
        }
        return "fragments/product-rows";
    }

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        logger.info("Viewing product detail for id={}", id);

        model.addAttribute("product", product);
        model.addAttribute("productCount", productRepository.count());
        return "product-detail";
    }

    @PostMapping("/products/sync")
    public String syncProducts(Model model) {
        logger.info("Manual product sync triggered via HTMX");
        try {
            productSyncService.forceSyncProducts();
            long count = productRepository.count();
            model.addAttribute("success", true);
            model.addAttribute("message", "Products synced successfully! Total products: " + count);
        } catch (Exception e) {
            logger.error("Error during manual sync: {}", e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "Error syncing products: " + e.getMessage());
        }
        return "fragments/sync-status";
    }

    @PostMapping("/products")
    public String addProduct(@RequestParam String title,
                           @RequestParam String handle,
                           @RequestParam BigDecimal price,
                           @RequestParam(required = false) String productType,
                           Model model) {
        logger.info("Adding new product: {}", title);
        
        try {
            ProductVariant variant = new ProductVariant(null, title, price, null, true);
            Product product = new Product(null, title, handle, price, productType, List.of(variant));
            productRepository.save(product);
            model.addAttribute("errorMessage", null);
            return renderProductPage(0, DEFAULT_PAGE_SIZE, model);

        } catch (Exception e) {
            logger.error("Error adding product: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return renderProductPage(0, DEFAULT_PAGE_SIZE, model);
        }
    }

    @GetMapping("/products/form")
    public String showProductForm() {
        return "fragments/product-form";
    }

    @GetMapping("/products/form-close")
    public String closeProductForm() {
        return "fragments/empty";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @RequestParam String title,
                                @RequestParam String handle,
                                @RequestParam BigDecimal price,
                                @RequestParam(required = false) String productType,
                                @RequestParam(value = "shopifyProductId", required = false) String shopifyProductIdValue,
                                RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        logger.info("Updating product id={} with new details", id);

        Long shopifyProductId = null;
        if (shopifyProductIdValue != null && !shopifyProductIdValue.isBlank()) {
            try {
                shopifyProductId = Long.valueOf(shopifyProductIdValue.trim());
            } catch (NumberFormatException ex) {
                redirectAttributes.addFlashAttribute("updateError", "Shopify product ID must be a number.");
                return "redirect:/products/" + id;
            }
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("updateError", "Price cannot be negative.");
            return "redirect:/products/" + id;
        }

        product.setShopifyProductId(shopifyProductId)
                .setTitle(title.trim())
                .setHandle(handle.trim())
                .setPrice(price)
                .setProductType(productType != null && !productType.isBlank() ? productType.trim() : null);

        productRepository.save(product);

        redirectAttributes.addFlashAttribute("updateSuccess", true);
        return "redirect:/products/" + id;
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @RequestParam(value = "q", required = false) String query,
                                @RequestParam(value = "page", required = false) Integer page,
                                @RequestParam(value = "size", required = false) Integer size,
                                @RequestHeader(value = "HX-Request", required = false) Boolean isHxRequest,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        productRepository.deleteById(id);
        logger.info("Deleted product id={} (title={})", id, product.getTitle());

        boolean hxRequest = Boolean.TRUE.equals(isHxRequest);

        if (hxRequest) {
            if (query != null) {
                return searchProducts(query, model);
            }
            return renderProductPage(page, size, model);
        }

        redirectAttributes.addFlashAttribute("productDeleted", true);
        return "redirect:/";
    }
}
