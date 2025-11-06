package com.athengaudio.backend.model;

import java.util.Date;

import lombok.Data;

@Data // Dùng Lombok cho getters/setters
public class Review {
    private String author; // Tên người đánh giá
    private int rating; // Số sao (1-5)
    private String comment; // Nội dung bình luận
    private Date createdAt;

    public Review() {
        this.createdAt = new Date();
    }

    public Review(String author, int rating, String comment) {
        this.author = author;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = new Date();
    }
}