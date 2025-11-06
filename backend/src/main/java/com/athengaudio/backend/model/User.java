package com.athengaudio.backend.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String email;
    private String name;
    private String phone;
    private String address;
    private String avatar;
    private String role;
    private String password;
    private Date createdAt;
    private Date updatedAt;
    private List<String> wishlist;

    // Fields for OTP
    private String otpCode;
    private Date otpExpiry;
    private Integer otpAttempts;
}