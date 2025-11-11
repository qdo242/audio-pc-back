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

            chatMessage.setFrom(senderId);
            chatMessage.setFromName(getUserName(senderId));

            if (!isAdmin) {
                chatMessage.setTo(adminId);
            }

            ChatMessage savedMessage = chatService.saveMessage(chatMessage);

            simpMessagingTemplate.convertAndSendToUser(savedMessage.getTo(), "/queue/reply", savedMessage);
            simpMessagingTemplate.convertAndSendToUser(savedMessage.getFrom(), "/queue/reply", savedMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.getHistory")
    public void getHistory(@Payload Map<String, String> payload, Principal principal) {
        try {
            String userId = (principal != null) ? principal.getName() : null;
            if (userId == null) return;

            String adminId = userService.findAdminUserId();
            String targetId;

            if (userId.equals(adminId)) {
                targetId = payload.get("targetUserId");
            } else {
                targetId = adminId;
            }

            if (targetId != null) {
                List<ChatMessage> history = chatService.getChatHistory(userId, targetId);
                simpMessagingTemplate.convertAndSendToUser(userId, "/queue/history", history);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<?> getConversations() {
        try {
            List<String> senders = mongoTemplate.findDistinct(new Query(), "from", "chat_messages", String.class);
            String adminId = userService.findAdminUserId();
            
            List<User> users = senders.stream()
                .filter(id -> !id.equals(adminId))
                .map(id -> userService.getUserById(id).orElse(null))
                .filter(u -> u != null)
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "users", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private String getUserName(String userId) {
        return userService.getUserById(userId).map(User::getName).orElse("Khách");
    }
}