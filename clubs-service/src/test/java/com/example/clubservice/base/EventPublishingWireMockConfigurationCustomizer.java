package com.example.clubservice.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.kafka.core.KafkaTemplate;

@TestComponent
public class EventPublishingWireMockConfigurationCustomizer implements WireMockConfigurationCustomizer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${monolith.entity-change-event-publisher.enabled}")
    private boolean monolithEntityChangeEventPublisherEnabled;

    @Value("${service.migration.entity-change-topic}")
    private String topicName;

    @Override
    public void customize(WireMockConfiguration config) {
        if(monolithEntityChangeEventPublisherEnabled)
            config.extensions(new TestMonolithEntityChangeEventPublisher(topicName,kafkaTemplate, objectMapper));
    }
}
