package com.athengaudio.backend.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.athengaudio.backend.model.ChatMessage;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {

    // [CŨ - Dùng cho User thường] Lịch sử chat 2 chiều cụ thể
    @Query("{ $or: [ { 'from': ?0, 'to': ?1 }, { 'from': ?1, 'to': ?0 } ] }")
    List<ChatMessage> findChatHistory(String userId1, String userId2, Sort sort);

    // [MỚI - Dùng cho Admin] 
    // Logic: Lấy tin nhắn liên quan đến User (?0)
    // VÀ: người gửi KHÔNG PHẢI là 'BOT'
    // VÀ: người nhận KHÔNG PHẢI là 'BOT'
    @Query("{ $and: [ " +
           "  { $or: [ { 'from': ?0 }, { 'to': ?0 } ] }, " + 
           "  { 'from': { $ne: 'BOT' } }, " + 
           "  { 'to': { $ne: 'BOT' } } " + 
           "] }")
    List<ChatMessage> findUserHistoryExcludingBot(String userId, Sort sort);
}