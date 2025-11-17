package com.athengaudio.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.athengaudio.backend.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    // Tìm thông báo cho 1 user, sắp xếp mới nhất lên trước
    List<Notification> findByUserIdOrderByTimestampDesc(String userId);

    // Đếm số thông báo chưa đọc
    long countByUserIdAndReadFalse(String userId);
}