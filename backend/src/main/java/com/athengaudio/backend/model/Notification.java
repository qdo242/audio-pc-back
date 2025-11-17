package com.athengaudio.backend.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String userId; // ID của người nhận (có thể là admin hoặc user)
    private String message; // Nội dung, ví dụ: "Đơn hàng #123 đã được xác nhận"
    private String link; // Đường dẫn khi click vào, ví dụ: "/orders/123"
    private boolean read = false;
    private Date timestamp = new Date();

    public Notification(String userId, String message, String link) {
        this.userId = userId;
        this.message = message;
        this.link = link;
    }
}