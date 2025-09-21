package com.respiroc.gregfullstack.controller;

import com.respiroc.gregfullstack.model.Product;
import com.respiroc.gregfullstack.model.ProductVariant;
import com.respiroc.gregfullstack.repository.ProductRepository;
import com.respiroc.gregfullstack.service.ProductSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

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
    public String loadProducts(Model model) {
        logger.info("Loading products via HTMX");
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        model.addAttribute("productCount", products.size());
        if (!model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", null);
        }
        return "fragments/product-rows";
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
            return loadProducts(model);

        } catch (Exception e) {
            logger.error("Error adding product: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return loadProducts(model);
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
}
