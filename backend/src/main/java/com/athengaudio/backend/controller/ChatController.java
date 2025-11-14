package com.athengaudio.backend.controller;

import java.security.Principal;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.athengaudio.backend.model.ChatMessage;
import com.athengaudio.backend.model.User;
import com.athengaudio.backend.service.BotService;
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
    private BotService botService;
    @Autowired
    private MongoTemplate mongoTemplate;

    public static final String BOT_ID = "BOT";
    public static final String ADMIN_TARGET = "ADMIN";

    // Logger
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    // === 1. WEBSOCKET CHAT ===

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            String senderId = (principal != null) ? principal.getName() : chatMessage.getFrom();
            if (senderId == null)
                return;

            String adminId = userService.findAdminUserId();
            boolean isAdmin = senderId.equals(adminId);

            chatMessage.setFrom(senderId);
            chatMessage.setFromName(getUserName(senderId));
            chatMessage.setTimestamp(System.currentTimeMillis());

            String destinationId = chatMessage.getTo();

            // --- CASE 1: CHAT VỚI BOT ---
            if (BOT_ID.equals(destinationId)) {
                // Lưu tin nhắn User
                ChatMessage savedUserMsg = chatService.saveMessage(chatMessage);
                simpMessagingTemplate.convertAndSend("/topic/user/" + senderId, savedUserMsg);

                // Gọi BotService (Sử dụng getBotResponse trả về String)
                String aiResponse = botService.getBotResponse(chatMessage.getContent());

                // Tạo tin nhắn Bot
                ChatMessage botReply = new ChatMessage();
                botReply.setFrom(BOT_ID);
                botReply.setFromName("Atheng AI");
                botReply.setTo(senderId);
                botReply.setContent(aiResponse);
                botReply.setTimestamp(System.currentTimeMillis());

                // Lưu & Gửi lại
                ChatMessage savedBotMsg = chatService.saveMessage(botReply);
                simpMessagingTemplate.convertAndSend("/topic/user/" + senderId, savedBotMsg);
                return;
            }

            // --- CASE 2: CHAT VỚI ADMIN ---
            if (ADMIN_TARGET.equals(destinationId) || !isAdmin) {
                chatMessage.setTo(adminId);
            }

            ChatMessage savedMessage = chatService.saveMessage(chatMessage);
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getTo(), savedMessage);
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getFrom(), savedMessage);

        } catch (Exception e) {
            log.error("Error in sendMessage: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.getHistory")
    public void getHistory(@Payload Map<String, String> payload, Principal principal) {
        try {
            String currentUserId = (principal != null) ? principal.getName() : null;
            if (currentUserId == null)
                return;

            String adminId = userService.findAdminUserId();
            String targetId = payload.get("targetUserId");
            if (targetId == null || targetId.isEmpty() || ADMIN_TARGET.equals(targetId)) {
                targetId = adminId;
            }

            List<ChatMessage> history = chatService.getChatHistory(currentUserId, targetId);
            simpMessagingTemplate.convertAndSend("/topic/user/" + currentUserId, history);
        } catch (Exception e) {
            log.error("Error in getHistory: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    // === 2. REST API CHO POSTMAN ===

    /**
     * API test bot đơn giản - cho Postman
     */
    @PostMapping("/api/chat/test-bot")
    @ResponseBody
    public ResponseEntity<?> testBot(@RequestBody Map<String, String> request) {
        try {
            String userMessage = request.get("message");
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Tin nhắn không được để trống"));
            }

            // SỬA: Tự đo thời gian và gọi method trả về String
            long startTime = System.currentTimeMillis();
            String aiResponse = botService.generateContent(userMessage.trim());
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userMessage", userMessage);
            response.put("botResponse", aiResponse);
            response.put("processingTimeMs", duration);
            response.put("cached", false); // BotService mới không dùng cache phức tạp

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in testBot API: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * API chat với bot và lưu vào database
     */
    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendChatMessage(@RequestBody Map<String, String> request) {
        try {
            String userMessage = request.get("message");
            String userId = request.get("userId");
            String userName = request.get("userName");

            if (userMessage == null || userMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Tin nhắn không được để trống"));
            }

            // Lưu tin nhắn user
            ChatMessage userChatMessage = new ChatMessage();
            userChatMessage.setFrom(userId != null ? userId : "anonymous");
            userChatMessage.setFromName(userName != null ? userName : "Người dùng");
            userChatMessage.setTo(BOT_ID);
            userChatMessage.setContent(userMessage);
            userChatMessage.setTimestamp(System.currentTimeMillis());

            ChatMessage savedUserMessage = chatService.saveMessage(userChatMessage);

            // SỬA: Gọi BotService (trả về String) và tự đo thời gian
            long startTime = System.currentTimeMillis();
            String aiResponse = botService.generateContent(userMessage.trim());
            long duration = System.currentTimeMillis() - startTime;

            // Lưu tin nhắn bot
            ChatMessage botChatMessage = new ChatMessage();
            botChatMessage.setFrom(BOT_ID);
            botChatMessage.setFromName("Atheng AI");
            botChatMessage.setTo(userId != null ? userId : "anonymous");
            botChatMessage.setContent(aiResponse);
            botChatMessage.setTimestamp(System.currentTimeMillis());

            ChatMessage savedBotMessage = chatService.saveMessage(botChatMessage);

            // Tạo response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userMessage", savedUserMessage);
            response.put("botMessage", savedBotMessage);
            response.put("processingTimeMs", duration);
            response.put("cached", false);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in sendChatMessage API: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/api/chat/history/{userId}")
    @ResponseBody
    public ResponseEntity<?> getChatHistory(@PathVariable String userId) {
        try {
            List<ChatMessage> history = chatService.getChatHistory(userId, BOT_ID);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId,
                    "history", history,
                    "count", history.size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/chat/bot-info")
    @ResponseBody
    public ResponseEntity<?> getBotInfo() {
        try {
            // BotService mới đã có method getServiceInfo (dummy)
            Map<String, Object> serviceInfo = botService.getServiceInfo();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "serviceInfo", serviceInfo,
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/chat/bot-cache")
    @ResponseBody
    public ResponseEntity<?> clearBotCache() {
        try {
            // SỬA: Dùng long thay vì int để khớp với BotService.getCacheSize()
            long previousSize = botService.getCacheSize();
            botService.clearCache();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa " + previousSize + " entries từ cache",
                    "previousCacheSize", previousSize,
                    "currentCacheSize", botService.getCacheSize()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
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
                    .filter(id -> !id.equals(adminId) && !id.equals(BOT_ID))
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

    @GetMapping("/api/chat/health")
    @ResponseBody
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Atheng Audio ChatBot",
                "timestamp", System.currentTimeMillis(),
                "botEnabled", true));
    }

    private String getUserName(String userId) {
        return userService.getUserById(userId).map(User::getName).orElse("Người dùng");
    }
}