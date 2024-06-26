package com.example.clubservice.dynamo.base;

import com.example.clubservice.dynamo.migration.EntityChangeEvent;
import com.example.clubservice.dynamo.migration.MigrationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestComponent
public class TestMonolithEntityChangeEventHandler {
    private CountDownLatch latchForChangeEventPublishes = new CountDownLatch(1);
    private Map<String, EntityChangeEvent> eventMap = new HashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MigrationProperties migrationProperties;

    public void reset() {
        latchForChangeEventPublishes = new CountDownLatch(1);
        eventMap.clear();
    }

    @KafkaListener(topics = "${service.migration.entity-change-topic}", groupId = "club-service-tests")
    public void handle(String message) throws JsonProcessingException {
        EntityChangeEvent entityChangeEvent = objectMapper.readValue(message, EntityChangeEvent.class);
        eventMap.put(entityChangeEvent.getAction(), entityChangeEvent);
        latchForChangeEventPublishes.countDown();
    }

    public void waitForEntityChangeEvenToBetPublished() {
        try {
            latchForChangeEventPublishes.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void verifyEntityChangeEvent(Object entity, String operation) {
        try {
            EntityChangeEvent entityChangeEvent = eventMap.get(operation);
            assertEquals(migrationProperties.getSourceOrigin(), entityChangeEvent.getOrigin());
            assertEquals(entity.getClass().getSimpleName(), entityChangeEvent.getType());
            assertEquals(operation, entityChangeEvent.getAction());
            assertEquals(entity, objectMapper.readValue(entityChangeEvent.getEntity(), entity.getClass()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
