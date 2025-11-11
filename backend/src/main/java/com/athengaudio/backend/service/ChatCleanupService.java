package com.athengaudio.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class ChatCleanupService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldMessages() {
        long cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        Query query = new Query(Criteria.where("timestamp").lt(cutoff));
        mongoTemplate.remove(query, "chat_messages");
    }
}