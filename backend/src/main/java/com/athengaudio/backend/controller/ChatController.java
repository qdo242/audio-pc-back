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

            String destinationId = chatMessage.getTo();

            // === CASE 1: CHAT VỚI BOT ===
            if (BOT_ID.equals(destinationId)) {
                ChatMessage savedUserMsg = chatService.saveMessage(chatMessage);
                simpMessagingTemplate.convertAndSend("/topic/user/" + senderId, savedUserMsg);

                // Gọi Gemini AI
                String aiResponse = botService.getBotResponse(chatMessage.getContent());

                ChatMessage botReply = new ChatMessage();
                botReply.setFrom(BOT_ID);
                botReply.setFromName("Atheng AI");
                botReply.setTo(senderId);
                botReply.setContent(aiResponse);

                ChatMessage savedBotMsg = chatService.saveMessage(botReply);
                simpMessagingTemplate.convertAndSend("/topic/user/" + senderId, savedBotMsg);
                return;
            }

            // === CASE 2: CHAT VỚI ADMIN ===
            // Nếu gửi tới "ADMIN" hoặc user thường gửi (mặc định là gửi admin) -> Đổi thành
            // ID thật
            if (ADMIN_TARGET.equals(destinationId) || !isAdmin) {
                chatMessage.setTo(adminId);
            }

            ChatMessage savedMessage = chatService.saveMessage(chatMessage);

            // Gửi cho cả người nhận và người gửi (để đồng bộ)
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getTo(), savedMessage);
            simpMessagingTemplate.convertAndSend("/topic/user/" + savedMessage.getFrom(), savedMessage);

        } catch (Exception e) {
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

            // Nếu target là ADMIN hoặc rỗng -> Lấy lịch sử với Admin thật
            if (targetId == null || targetId.isEmpty() || ADMIN_TARGET.equals(targetId)) {
                targetId = adminId;
            }

            List<ChatMessage> history = chatService.getChatHistory(currentUserId, targetId);
            simpMessagingTemplate.convertAndSend("/topic/user/" + currentUserId, history);

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

    private String getUserName(String userId) {
        return userService.getUserById(userId).map(User::getName).orElse("Người dùng");
    }
}