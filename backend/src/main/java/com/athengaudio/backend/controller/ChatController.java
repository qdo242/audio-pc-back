package com.athengaudio.backend.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.athengaudio.backend.model.ChatMessage;
import com.athengaudio.backend.model.User; // <-- THÊM IMPORT NÀY
import com.athengaudio.backend.service.ChatService;
import com.athengaudio.backend.service.UserService;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private ChatService chatService;
    @Autowired
    private UserService userService;

    // Hàm helper (giữ nguyên)
    private String getUserName(String userId) {
        Optional<User> userOpt = userService.getUserById(userId);
        return userOpt.map(User::getName).orElse("Người dùng");
    }

    /**
     * Client gửi tin nhắn đến "/app/chat.sendMessage"
     */
    // === SỬA ĐỔI: Thêm Principal principal ===
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        
        String senderId = principal.getName(); // <-- Lấy ID người gửi TỪ KẾT NỐI
        String adminId = userService.findAdminUserId();
        
        // Đảm bảo tin nhắn có thông tin chính xác
        chatMessage.setFrom(senderId); 
        chatMessage.setTo(adminId);
        chatMessage.setFromName(getUserName(senderId)); // Lấy tên người gửi

        ChatMessage savedMessage = chatService.saveMessage(chatMessage);

        // Gửi cho Admin
        simpMessagingTemplate.convertAndSendToUser(
            savedMessage.getTo(), 
            "/queue/reply", 
            savedMessage
        );

        // Gửi lại cho người gửi (chính là principal)
        simpMessagingTemplate.convertAndSendToUser(
            savedMessage.getFrom(), // Vẫn là senderId
            "/queue/reply", 
            savedMessage
        );
    }

    /**
     * Client gửi yêu cầu lấy lịch sử đến "/app/chat.getHistory"
     */
    // === SỬA ĐỔI: Thay @Payload String userId BẰNG Principal principal ===
    @MessageMapping("/chat.getHistory")
    public void getHistory(Principal principal) {
        
        String userId = principal.getName(); // <-- Lấy ID người dùng TỪ KẾT NỐI
        String adminId = userService.findAdminUserId();
        
        List<ChatMessage> history = chatService.getChatHistory(userId, adminId);

        // Thêm tên cho các tin nhắn cũ
        history.forEach(message -> {
            message.setFromName(getUserName(message.getFrom()));
        });

        // Gửi lịch sử về cho user (chính là principal)
        simpMessagingTemplate.convertAndSendToUser(
            userId, 
            "/queue/history", 
            history
        );
    }
}