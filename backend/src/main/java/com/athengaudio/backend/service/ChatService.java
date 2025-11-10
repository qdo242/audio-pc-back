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

    /**
     * Lưu một tin nhắn mới vào database
     */
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        chatMessage.setTimestamp(System.currentTimeMillis());
        return chatRepository.save(chatMessage);
    }

    /**
     * Lấy lịch sử chat giữa 2 người, sắp xếp theo thời gian tăng dần
     */
    public List<ChatMessage> getChatHistory(String userId1, String userId2) {
        return chatRepository.findChatHistory(userId1, userId2, 
            Sort.by(Sort.Direction.ASC, "timestamp")
        );
    }
}