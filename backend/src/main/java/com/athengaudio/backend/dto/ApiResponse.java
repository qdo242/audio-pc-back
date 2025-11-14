package com.athengaudio.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ApiResponse {
    private String status;
    private String message;
    private Object data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }

    public ApiResponse(String status, String message, Object data) {
        this();
        this.status = status;
        this.message = message;
        this.data = data;
    }
}