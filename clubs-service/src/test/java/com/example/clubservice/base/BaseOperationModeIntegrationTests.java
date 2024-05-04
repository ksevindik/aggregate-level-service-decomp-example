package com.example.clubservice.base;

import com.example.clubservice.migration.EntityChangeEvent;
import com.example.clubservice.migration.EntityPersistedEvent;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.utils.EventPublishingWireMockConfigurationCustomizer;
import com.example.clubservice.utils.MapProxy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(BaseOperationModeIntegrationTests.BaseTestConfig.class)
public abstract class BaseOperationModeIntegrationTests extends BaseIntegrationTests {
    protected abstract OperationMode getOperationMode();

    @Autowired
    protected OperationModeManager operationModeManager;

    @TestConfiguration
    static class BaseTestConfig {
        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper)  {
            return new EventPublishingWireMockConfigurationCustomizer(kafkaTemplate, objectMapper);
        }
    }


    private static CountDownLatch latchForChangeEventPublishes = new CountDownLatch(1);
    private static CountDownLatch latchForEntityPersistedEvents = new CountDownLatch(1);
    private static Map<String, EntityChangeEvent> eventMap = MapProxy.createProxy(new HashMap<>());

    @BeforeEach
    public void __setUp() {
        operationModeManager.setOperationMode(getOperationMode());
        latchForChangeEventPublishes = new CountDownLatch(1);
        latchForEntityPersistedEvents = new CountDownLatch(1);
    }

    @AfterAll
    static void _afterAll() {
        eventMap.clear();
    }

    @KafkaListener(topics = "entity-change-topic", groupId = "club-service-tests")
    public void handle(String message) throws JsonProcessingException {
        EntityChangeEvent entityChangeEvent = objectMapper.readValue(message, EntityChangeEvent.class);
        eventMap.put(entityChangeEvent.getAction(), entityChangeEvent);
        latchForChangeEventPublishes.countDown();
    }

    @TransactionalEventListener
    public void handle(EntityPersistedEvent event) {
        latchForEntityPersistedEvents.countDown();
    }

    protected void waitForEntityChangeEvenToBetPublished() {
        try {
            latchForChangeEventPublishes.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void waitForEntityPersistedEvent() {
        try {
            latchForEntityPersistedEvents.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void verifyEntityChangeEvent(Object entity, String operation) {
        try {
            EntityChangeEvent entityChangeEvent = eventMap.get(operation);
            assertEquals("service", entityChangeEvent.getOrigin());
            assertEquals(entity.getClass().getSimpleName(), entityChangeEvent.getType());
            assertEquals(operation, entityChangeEvent.getAction());
            assertEquals(entity, objectMapper.readValue(entityChangeEvent.getEntity(), entity.getClass()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
