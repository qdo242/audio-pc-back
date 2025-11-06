package com.athengaudio.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.athengaudio.backend.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    // Tìm user bằng email
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa
    Boolean existsByEmail(String email);

    // Tìm user theo role
    List<User> findByRole(String role);

    // Tìm user theo tên (tìm kiếm không phân biệt hoa thường)
    List<User> findByNameContainingIgnoreCase(String name);

    // Tìm user có wishlist chứa productId cụ thể
    @Query("{ 'wishlist': ?0 }")
    List<User> findByProductInWishlist(String productId);

    // Đếm số user theo role
    long countByRole(String role);

}