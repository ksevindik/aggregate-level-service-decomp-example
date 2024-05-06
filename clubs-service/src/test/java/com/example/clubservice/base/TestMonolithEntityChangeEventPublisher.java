package com.example.clubservice.base;

import com.example.clubservice.migration.EntityChangeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.Response;
import org.springframework.kafka.core.KafkaTemplate;

public class TestMonolithEntityChangeEventPublisher extends ResponseTransformer {

    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    public TestMonolithEntityChangeEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Response transform(Request request, Response response, FileSource fileSource, Parameters parameters) {
        try {
            return response;
        } finally {
            if((request.getMethod().equals(RequestMethod.PUT) ||
                    request.getMethod().equals(RequestMethod.POST) ||
                    request.getMethod().equals(RequestMethod.DELETE)) &&
                    (response.getStatus() == 200 || response.getStatus() == 201)) {

                String action = "UPDATE";
                if(request.getMethod().equals(RequestMethod.POST)) {
                    action = "CREATE";
                } else if(request.getMethod().equals(RequestMethod.DELETE)) {
                    action = "DELETE";
                }
                String type = request.getUrl().startsWith("/clubs") ? "Club" : "Player";
                publishEntityChangeEventFromMonolith(response.getBodyAsString(),  type,action);
            }
        }
    }

    @Override
    public String getName() {
        return "monolith-entity-change-event-publisher";
    }

    protected void publishEntityChangeEventFromMonolith(String entity, String type, String operation) {
        try {
            EntityChangeEvent entityChangeEvent = new EntityChangeEvent(operation, type, "monolith", entity);
            kafkaTemplate.send("entity-change-topic", objectMapper.writeValueAsString(entityChangeEvent));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
