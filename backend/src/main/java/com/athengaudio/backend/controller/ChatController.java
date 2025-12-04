package com.athengaudio.backend.controller;

import java.security.Principal;
import java.util.ArrayList;
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
import com.athengaudio.backend.model.Notification;
import com.athengaudio.backend.model.User;
import com.athengaudio.backend.service.BotService;
import com.athengaudio.backend.service.ChatService;
import com.athengaudio.backend.service.NotificationService;
import com.athengaudio.backend.service.UserService;

@Controller
public class ChatController {

    @Autowired private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired private ChatService chatService;
    @Autowired private UserService userService;
    @Autowired private BotService botService;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private NotificationService notificationService;

    public static final String BOT_ID = "BOT";
    public static final String ADMIN_TARGET = "ADMIN"; // Frontend gửi to: "ADMIN"

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    // === 1. WEBSOCKET (Xử lý tin nhắn) ===
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            String senderId = (principal != null) ? principal.getName() : chatMessage.getFrom();
            if (senderId == null) return;

            // Tìm ID thật của Admin trong DB
            String adminId = userService.findAdminUserId();
            boolean isAdmin = senderId.equals(adminId);

            chatMessage.setFrom(senderId);
            chatMessage.setFromName(getUserName(senderId));
            chatMessage.setTimestamp(System.currentTimeMillis());

            String destinationId = chatMessage.getTo();

            // --- CASE 1: CHAT VỚI BOT ---
            if (BOT_ID.equals(destinationId)) {
                // Lưu tin nhắn User -> Bot
                ChatMessage savedUserMsg = chatService.saveMessage(chatMessage);
                simpMessagingTemplate.convertAndSend("/topic/user/" + senderId, savedUserMsg);

                // Bot trả lời
                String aiResponse = botService.getBotResponse(chatMessage.getContent());
                ChatMessage botReply = new ChatMessage();
                botReply.setFrom(BOT_ID);
                botReply.setFromName("Atheng AI");
                botReply.setTo(senderId);
                botReply.setContent(aiResponse);
                botReply.setTimestamp(System.currentTimeMillis());

                ChatMessage savedBotMsg = chatService.saveMessage(botReply);
                simpMessagingTemplate.convertAndSend("/topic/user/" + senderId, savedBotMsg);
                return;
            }

            // --- CASE 2: CHAT VỚI ADMIN ---
            // Nếu Frontend gửi to="ADMIN" hoặc người gửi không phải là Admin (tức là User muốn chat support)
            if (ADMIN_TARGET.equals(destinationId) || (!isAdmin && !BOT_ID.equals(destinationId))) {
                // Ép buộc người nhận là ID thật của Admin
                chatMessage.setTo(adminId);
            }

            // Lưu vào MongoDB
            ChatMessage savedMessage = chatService.saveMessage(chatMessage);
            
            // Gửi Realtime
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getTo(), savedMessage); // Gửi cho người nhận
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getFrom(), savedMessage); // Gửi lại cho người gửi (để hiện UI)

            // Thông báo
            if (isAdmin) {
                Notification notif = new Notification(savedMessage.getTo(), "Hỗ trợ viên: " + savedMessage.getContent(), "/profile");
                notificationService.sendNotificationToUser(notif);
            } else {
                Notification notif = new Notification(adminId, "Tin nhắn từ " + savedMessage.getFromName(), "/admin");
                notificationService.sendNotificationToUser(notif);
            }

        } catch (Exception e) {
            log.error("Lỗi gửi tin nhắn", e);
        }
    }

    // === 2. REST API (Lấy lịch sử) ===

    // [FIXED] API Admin lấy lịch sử (Đã loại bỏ tin nhắn BOT)
    @GetMapping("/api/chat/admin/history/{userId}")
    @ResponseBody
    public ResponseEntity<?> getAdminUserChatHistory(@PathVariable String userId) {
        try {
            // Gọi hàm service mới đã filter BOT
            List<ChatMessage> history = chatService.getAdminUserHistory(userId);
            return ResponseEntity.ok(Map.of("success", true, "history", history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // API Lấy danh sách hội thoại (Cần lọc bỏ những User chỉ chat với Bot nếu muốn, nhưng tạm thời cứ lấy hết User trừ Bot/Admin)
    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<?> getConversations() {
        try {
            String adminId = userService.findAdminUserId();
            
            // Query distinct users from messages, excluding BOT messages from the conversation list logic if desired
            // Tuy nhiên, để đơn giản ta lấy tất cả những ai đã từng có tin nhắn trong hệ thống
            
            List<String> senders = mongoTemplate.findDistinct(new Query(), "from", "chat_messages", String.class);
            List<String> receivers = mongoTemplate.findDistinct(new Query(), "to", "chat_messages", String.class);
            senders.addAll(receivers);

            List<String> userIds = senders.stream()
                    .distinct()
                    .filter(id -> id != null && !id.equals(adminId) && !id.equals(BOT_ID) && !id.equals(ADMIN_TARGET))
                    .collect(Collectors.toList());

            List<User> users = new ArrayList<>();
            for (String id : userIds) {
                userService.getUserById(id).ifPresent(user -> {
                    user.setPassword(null);
                    users.add(user);
                });
            }

            return ResponseEntity.ok(Map.of("success", true, "users", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // API User xem lịch sử (Mặc định xem với Bot, hoặc có thể thêm API xem với Admin)
    @GetMapping("/api/chat/history/{userId}")
    @ResponseBody
    public ResponseEntity<?> getChatHistory(@PathVariable String userId) {
        try {
            // Mặc định User xem lại lịch sử với BOT. 
            // Nếu muốn User xem lịch sử với Admin thì cần thêm endpoint khác hoặc gộp chung.
            // Ở đây giữ nguyên logic cũ là xem với BOT cho widget mặc định.
            List<ChatMessage> history = chatService.getChatHistory(userId, BOT_ID);
            return ResponseEntity.ok(Map.of("success", true, "history", history));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ... Các API khác giữ nguyên ...
    @PostMapping("/api/chat/send") @ResponseBody public ResponseEntity<?> sendChatMessage(@RequestBody Map<String, String> r) { return ResponseEntity.ok().build(); }
    @PostMapping("/api/chat/test-bot") @ResponseBody public ResponseEntity<?> testBot(@RequestBody Map<String, String> r) { return ResponseEntity.ok().build(); }
    @DeleteMapping("/api/chat/bot-cache") @ResponseBody public ResponseEntity<?> clearBotCache() { return ResponseEntity.ok().build(); }
    @GetMapping("/api/chat/bot-info") @ResponseBody public ResponseEntity<?> getBotInfo() { return ResponseEntity.ok().build(); }
    @GetMapping("/api/chat/health") @ResponseBody public ResponseEntity<?> healthCheck() { return ResponseEntity.ok(Map.of("status", "UP")); }

    private String getUserName(String userId) {
        return userService.getUserById(userId).map(User::getName).orElse("Người dùng");
    }
}