package com.athengaudio.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.ContactMessage;
import com.athengaudio.backend.repository.ContactMessageRepository;

@Service
public class ContactService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private EmailService emailService;

    public ContactMessage saveMessage(ContactMessage message) {
        // 1. Lưu tin nhắn vào DB
        ContactMessage savedMessage = contactMessageRepository.save(message);

        // 2. Gửi email thông báo cho admin
        try {
            emailService.sendContactFormNotification(savedMessage);
        } catch (Exception e) {
            // Ghi log lỗi gửi mail, nhưng không cần chặn request
            System.out.println("Error sending contact email notification: " + e.getMessage());
        }

        return savedMessage;
    }
}