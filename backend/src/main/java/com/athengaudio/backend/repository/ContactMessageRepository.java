package com.athengaudio.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.athengaudio.backend.model.ContactMessage;

public interface ContactMessageRepository extends MongoRepository<ContactMessage, String> {
}