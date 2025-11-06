package com.athengaudio.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.athengaudio.backend.model.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);

    List<Product> findByBrand(String brand);

    List<Product> findByIsActiveTrue();

    List<Product> findByIsFeaturedTrue();

    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Product> findByCategoryAndBrand(String category, String brand);

    @Query("{'originalPrice': {$gt: '$price'}}")
    List<Product> findDiscountedProducts();

    List<Product> findByStockGreaterThan(Integer stock);

    @Query("{'tags': {$in: [?0]}}")
    List<Product> findByTag(String tag);

    List<Product> findByRatingGreaterThanEqual(Double rating);

    // Additional methods for active products
    List<Product> findByCategoryAndIsActiveTrue(String category);

    List<Product> findByBrandAndIsActiveTrue(String brand);

    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    List<Product> findByIsFeaturedTrueAndIsActiveTrue();

    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findByCategoryAndIdNot(String category, String id, Pageable pageable);
}