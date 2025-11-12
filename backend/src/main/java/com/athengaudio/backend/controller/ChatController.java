package com.athengaudio.backend.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.athengaudio.backend.model.ChatMessage;
import com.athengaudio.backend.model.User;
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
    @Autowired
    private MongoTemplate mongoTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            String senderId = (principal != null) ? principal.getName() : chatMessage.getFrom();
            if (senderId == null) return;

            String adminId = userService.findAdminUserId();
            boolean isAdmin = senderId.equals(adminId);

            // Chuẩn hóa thông tin
            chatMessage.setFrom(senderId);
            chatMessage.setFromName(getUserName(senderId));

            // Nếu User gửi -> Đích đến là Admin
            if (!isAdmin) {
                chatMessage.setTo(adminId);
            }
            // Nếu Admin gửi -> chatMessage.getTo() đã có ID khách hàng

            // Lưu DB
            ChatMessage savedMessage = chatService.saveMessage(chatMessage);

            // Gửi cho người nhận (Destination)
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getTo(), savedMessage);
            
            // Gửi lại cho người gửi (Source) để hiển thị lên UI
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getFrom(), savedMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.getHistory")
    public void getHistory(@Payload Map<String, String> payload, Principal principal) {
        try {
            String currentUserId = (principal != null) ? principal.getName() : null;
            if (currentUserId == null) return;

            String adminId = userService.findAdminUserId();
            String targetId;

            if (currentUserId.equals(adminId)) {
                targetId = payload.get("targetUserId");
            } else {
                targetId = adminId;
            }

            if (targetId != null) {
                List<ChatMessage> history = chatService.getChatHistory(currentUserId, targetId);
                simpMessagingTemplate.convertAndSend("/topic/user/" + currentUserId, history);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<?> getConversations() {
        try {
            String adminId = userService.findAdminUserId();
            
            List<String> senders = mongoTemplate.findDistinct(new Query(), "from", "chat_messages", String.class);
            List<String> receivers = mongoTemplate.findDistinct(new Query(), "to", "chat_messages", String.class);
            
            senders.addAll(receivers);
            List<String> userIds = senders.stream()
                .distinct()
                .filter(id -> !id.equals(adminId))
                .collect(Collectors.toList());

            List<User> users = userIds.stream()
                .map(id -> userService.getUserById(id).orElse(null))
                .filter(u -> u != null)
                .peek(u -> u.setPassword(null))
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "users", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private String getUserName(String userId) {
        return userService.getUserById(userId).map(User::getName).orElse("Người dùng");
    }
}