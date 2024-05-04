package com.example.clubservice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.kafka.core.KafkaTemplate;

public class EventPublishingWireMockConfigurationCustomizer implements WireMockConfigurationCustomizer {

    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    @Value("${monolith.entity-change-event-publisher.enabled}")
    private boolean monolithEntityChangeEventPublisherEnabled;

    public EventPublishingWireMockConfigurationCustomizer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void customize(WireMockConfiguration config) {
        if(monolithEntityChangeEventPublisherEnabled)
            config.extensions(new TestMonolithEntityChangeEventPublisher(kafkaTemplate, objectMapper));
    }
}
