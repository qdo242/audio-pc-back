package com.athengaudio.backend.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.Product;
import com.athengaudio.backend.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrue();
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());
        product.setIsActive(true);
        if (product.getReviewCount() == null) {
            product.setReviewCount(0);
        }
        if (product.getRating() == null) {
            product.setRating(0.0);
        }

        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    if (productDetails.getName() != null)
                        product.setName(productDetails.getName());
                    if (productDetails.getDescription() != null)
                        product.setDescription(productDetails.getDescription());
                    if (productDetails.getCategory() != null)
                        product.setCategory(productDetails.getCategory());
                    if (productDetails.getBrand() != null)
                        product.setBrand(productDetails.getBrand());
                    if (productDetails.getPrice() != null)
                        product.setPrice(productDetails.getPrice());
                    if (productDetails.getOriginalPrice() != null)
                        product.setOriginalPrice(productDetails.getOriginalPrice());
                    if (productDetails.getStock() != null)
                        product.setStock(productDetails.getStock());
                    if (productDetails.getImages() != null)
                        product.setImages(productDetails.getImages());
                    if (productDetails.getColors() != null)
                        product.setColors(productDetails.getColors());
                    if (productDetails.getSizes() != null)
                        product.setSizes(productDetails.getSizes());
                    if (productDetails.getConnectivity() != null)
                        product.setConnectivity(productDetails.getConnectivity());
                    if (productDetails.getBatteryLife() != null)
                        product.setBatteryLife(productDetails.getBatteryLife());
                    if (productDetails.getWeight() != null)
                        product.setWeight(productDetails.getWeight());
                    if (productDetails.getCompatibility() != null)
                        product.setCompatibility(productDetails.getCompatibility());
                    if (productDetails.getWarranty() != null)
                        product.setWarranty(productDetails.getWarranty());
                    if (productDetails.getFeatures() != null)
                        product.setFeatures(productDetails.getFeatures());
                    if (productDetails.getTags() != null)
                        product.setTags(productDetails.getTags());
                    if (productDetails.getIsFeatured() != null)
                        product.setIsFeatured(productDetails.getIsFeatured());
                    if (productDetails.getRating() != null)
                        product.setRating(productDetails.getRating());
                    if (productDetails.getReviewCount() != null)
                        product.setReviewCount(productDetails.getReviewCount());

                    product.setUpdatedAt(new Date());
                    return productRepository.save(product);
                })
                .orElse(null);
    }

    public boolean deleteProduct(String id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setIsActive(false);
                    product.setUpdatedAt(new Date());
                    productRepository.save(product);
                    return true;
                })
                .orElse(false);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndIsActiveTrue(category);
    }

    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrandAndIsActiveTrue(brand);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword);
    }

    public List<Product> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrue();
    }

    public List<Product> getDiscountedProducts() {
        return productRepository.findDiscountedProducts();
    }

    public List<Product> getNewestProducts(int limit) {
        return productRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    public Product updateRating(String productId, Double newRating) {
        return productRepository.findById(productId)
                .map(product -> {
                    int currentReviewCount = product.getReviewCount();
                    double currentRating = product.getRating();

                    double newAverageRating = ((currentRating * currentReviewCount) + newRating)
                            / (currentReviewCount + 1);

                    product.setRating(newAverageRating);
                    product.setReviewCount(currentReviewCount + 1);
                    product.setUpdatedAt(new Date());

                    return productRepository.save(product);
                })
                .orElse(null);
    }

    public Product updateStock(String productId, Integer newStock) {
        return productRepository.findById(productId)
                .map(product -> {
                    product.setStock(newStock);
                    product.setUpdatedAt(new Date());
                    return productRepository.save(product);
                })
                .orElse(null);
    }

}