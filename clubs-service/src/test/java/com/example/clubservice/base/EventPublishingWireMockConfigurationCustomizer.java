package com.example.clubservice.base;

import com.example.clubservice.migration.MigrationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.kafka.core.KafkaTemplate;

@TestComponent
public class EventPublishingWireMockConfigurationCustomizer implements WireMockConfigurationCustomizer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${service.test.monolith.entity-change-event-publisher.enabled:false}")
    private boolean monolithEntityChangeEventPublisherEnabled;

    @Autowired
    private MigrationProperties migrationProperties;

    @Override
    public void customize(WireMockConfiguration config) {
        if(monolithEntityChangeEventPublisherEnabled)
            config.extensions(new TestMonolithEntityChangeEventPublisher(migrationProperties,kafkaTemplate, objectMapper));
    }
}
