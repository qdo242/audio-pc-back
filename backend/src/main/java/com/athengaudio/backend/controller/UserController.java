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
import org.springframework.web.bind.annotation.RestController;

import com.athengaudio.backend.model.User;
import com.athengaudio.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;

    // Lấy thông tin user (ví dụ: cho trang hồ sơ)
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            user.get().setPassword(null); // Không trả về mật khẩu
            return ResponseEntity.ok(Map.of("success", true, "user", user.get()));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy người dùng"));
    }

    // Cập nhật hồ sơ (Tên, SĐT, Địa chỉ)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(@PathVariable String id, @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        if (updatedUser != null) {
            updatedUser.setPassword(null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", updatedUser,
                    "message", "Cập nhật hồ sơ thành công"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cập nhật thất bại"));
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        boolean success = userService.changePassword(
                id,
                request.getCurrentPassword(),
                request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đổi mật khẩu thành công"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Mật khẩu hiện tại không đúng"));
        }
    }

    // --- API WISHLIST ---

    @GetMapping("/{userId}/wishlist")
    public ResponseEntity<?> getUserWishlist(@PathVariable String userId) {
        List<String> wishlist = userService.getWishlist(userId);
        return ResponseEntity.ok(Map.of("success", true, "wishlist", wishlist));
    }

    @PostMapping("/{userId}/wishlist/{productId}")
    public ResponseEntity<?> addToWishlist(@PathVariable String userId, @PathVariable String productId) {
        User user = userService.addProductToWishlist(userId, productId);
        if (user != null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã thêm vào yêu thích"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thêm thất bại"));
    }

    @DeleteMapping("/{userId}/wishlist/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable String userId, @PathVariable String productId) {
        User user = userService.removeProductFromWishlist(userId, productId);
        if (user != null) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa khỏi yêu thích"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Xóa thất bại"));
    }
}