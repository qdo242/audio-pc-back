package com.athengaudio.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.ChatMessage;
import com.athengaudio.backend.repository.ChatRepository;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    // Lưu tin nhắn
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        if (chatMessage.getTimestamp() == 0) {
            chatMessage.setTimestamp(System.currentTimeMillis());
        }
        return chatRepository.save(chatMessage);
    }

    // Lấy lịch sử chat cụ thể giữa 2 người (VD: User xem chat với Bot)
    public List<ChatMessage> getChatHistory(String userId1, String userId2) {
        return chatRepository.findChatHistory(userId1, userId2, Sort.by(Sort.Direction.ASC, "timestamp"));
    }

    // [SỬA] Lấy lịch sử chat cho Admin (Loại bỏ tin nhắn Bot)
    public List<ChatMessage> getAdminUserHistory(String userId) {
        // Gọi hàm mới đã loại bỏ BOT
        return chatRepository.findUserHistoryExcludingBot(userId, Sort.by(Sort.Direction.ASC, "timestamp"));
    }
}