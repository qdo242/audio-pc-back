package com.athengaudio.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.Notification;
import com.athengaudio.backend.repository.NotificationRepository;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Service để gửi WebSocket

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService; // Để tìm ID admin

    /**
     * Gửi thông báo cho một User ID cụ thể
     */
    public void sendNotificationToUser(Notification notification) {
        // 1. Lưu vào DB
        Notification savedNotification = notificationRepository.save(notification);
        
        // 2. Gửi qua WebSocket đến kênh riêng của user đó
        // Kênh này sẽ là /topic/notifications/USER_ID
        String destination = "/topic/notifications/" + notification.getUserId();
        messagingTemplate.convertAndSend(destination, savedNotification);
    }

    /**
     * Gửi thông báo cho TẤT CẢ Admin
     */
    public void notifyAdmin(Notification notification) {
        String adminId = userService.findAdminUserId(); 
        if (adminId != null) {
            notification.setUserId(adminId); // Đặt người nhận là Admin
            sendNotificationToUser(notification); // Gửi
        }
    }
}