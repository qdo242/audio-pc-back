package com.athengaudio.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.athengaudio.backend.model.Product;
import com.athengaudio.backend.service.ProductService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")

public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API đang hoạt động!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable String id) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "product", product.get()));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy sản phẩm"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy sản phẩm: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Product createdProduct = productService.createProduct(product);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "product", createdProduct,
                    "message", "Tạo sản phẩm thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tạo sản phẩm: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        try {
            Product updatedProduct = productService.updateProduct(id, productDetails);
            if (updatedProduct != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "product", updatedProduct,
                        "message", "Cập nhật sản phẩm thành công"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy sản phẩm"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật sản phẩm: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        try {
            boolean deleted = productService.deleteProduct(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Xóa sản phẩm thành công"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy sản phẩm"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable String category) {
        try {
            List<Product> products = productService.getProductsByCategory(category);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy sản phẩm theo category: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String q) {
        try {
            List<Product> products = productService.searchProducts(q);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size(),
                    "query", q));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tìm kiếm sản phẩm: " + e.getMessage()));
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedProducts() {
        try {
            List<Product> products = productService.getFeaturedProducts();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy sản phẩm nổi bật: " + e.getMessage()));
        }
    }

    @GetMapping("/discounted")
    public ResponseEntity<?> getDiscountedProducts() {
        try {
            List<Product> products = productService.getDiscountedProducts();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy sản phẩm giảm giá: " + e.getMessage()));
        }
    }

    @GetMapping("/newest")
    public ResponseEntity<?> getNewestProducts(@RequestParam(defaultValue = "8") int limit) {
        try {
            List<Product> products = productService.getNewestProducts(limit);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy sản phẩm mới nhất: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<?> updateRating(@PathVariable String id, @RequestBody Map<String, Double> request) {
        try {
            Double newRating = request.get("rating");
            if (newRating == null || newRating < 0 || newRating > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Rating phải từ 0 đến 5"));
            }

            Product updatedProduct = productService.updateRating(id, newRating);
            if (updatedProduct != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "product", updatedProduct,
                        "message", "Cập nhật rating thành công"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy sản phẩm"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật rating: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable String id, @RequestBody Map<String, Integer> request) {
        try {
            Integer newStock = request.get("stock");
            if (newStock == null || newStock < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Stock không hợp lệ"));
            }

            Product updatedProduct = productService.updateStock(id, newStock);
            if (updatedProduct != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "product", updatedProduct,
                        "message", "Cập nhật stock thành công"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không tìm thấy sản phẩm"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật stock: " + e.getMessage()));
        }
    }
}