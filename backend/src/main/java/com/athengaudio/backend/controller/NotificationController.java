package com.athengaudio.backend.controller;

import com.athengaudio.backend.model.Notification;
import com.athengaudio.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    // Lấy tất cả thông báo của user đang đăng nhập
    @GetMapping
    public ResponseEntity<?> getMyNotifications(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String userId = principal.getName(); // Lấy userId từ principal
        List<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(userId);
        return ResponseEntity.ok(Map.of("success", true, "notifications", notifications));
    }

    // Đếm số thông báo chưa đọc
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("success", true, "count", 0));
        }
        String userId = principal.getName();
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }

    // Đánh dấu đã đọc
    @PostMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable String id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String userId = principal.getName();
        return notificationRepository.findById(id)
            .map(notification -> {
                // Đảm bảo đúng là chủ nhân của thông báo
                if (!notification.getUserId().equals(userId)) {
                    return ResponseEntity.status(403).build();
                }
                notification.setRead(true);
                notificationRepository.save(notification);
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}