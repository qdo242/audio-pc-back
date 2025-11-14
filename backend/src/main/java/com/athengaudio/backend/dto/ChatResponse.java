package com.athengaudio.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatResponse {
    private String response;
    private String conversationId;
    private LocalDateTime timestamp;
    private String status;
    private String message;
    private Long processingTimeMs;

    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatResponse(String response, String conversationId) {
        this();
        this.response = response;
        this.conversationId = conversationId;
        this.status = "success";
    }
}