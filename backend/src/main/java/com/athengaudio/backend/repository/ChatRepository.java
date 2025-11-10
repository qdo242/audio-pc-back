package com.athengaudio.backend.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.athengaudio.backend.model.ChatMessage;

public interface ChatRepository extends MongoRepository<ChatMessage, String> {

    /**
     * Tìm tất cả tin nhắn giữa hai người dùng, sắp xếp theo thời gian
     * ?0 là userId1, ?1 là userId2
     */
    @Query("{$or: [ { 'from': ?0, 'to': ?1 }, { 'from': ?1, 'to': ?0 } ] }")
    List<ChatMessage> findChatHistory(String userId1, String userId2, Sort sort);
}