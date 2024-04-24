package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.IdMappingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
the important point here is NOT to consume the messages whose origin is the same as the monolith itself,
and NOT to publish a change event in case we handle that event in order not to create an endless change event loop.
 */
@Component
public class EntityChangeEventListener {

    @Autowired
    private EntityPersister entityPersister;

    @Autowired
    private IdMappingRepository idMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationModeManager operationModeManager;

    private String targetOrigin = "monolith";

    @KafkaListener(topics = "entity-change-topic",groupId = "club-service")
    public void listen(String message) {
        try {
            EntityChangeEvent event = objectMapper.readValue(message, EntityChangeEvent.class);
            //we should discard the change event if operation mode is other than read-only.
            if(!operationModeManager.isReadOnly()) return;

            if (targetOrigin.equals(event.getOrigin())) {
                switch (event.getType()) {
                    case "Club":
                        processClubEvent(event);
                        break;
                    case "Player":
                        processPlayerEvent(event);
                        break;
                    default:
                        System.out.println("Unsupported entity type :" + event.getType());
                        break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process entity change event", e);
        }
    }

    private void processClubEvent(EntityChangeEvent event) throws JsonProcessingException {
        Club monolithClub = objectMapper.readValue(event.getEntity(), Club.class);
        switch (event.getAction()) {
            case "CREATE":
                entityPersister.createFrom(monolithClub);
                break;
            case "UPDATE":
                entityPersister.updateFrom(monolithClub);
                break;
            case "DELETE":
                entityPersister.deleteFrom(monolithClub);
                break;
            default:
                throw new IllegalStateException("Unsupported action for club event handler: " + event.getAction());
        }
    }

    private void processPlayerEvent(EntityChangeEvent event) throws JsonProcessingException {
        Player monolithPlayer = objectMapper.readValue(event.getEntity(), Player.class);
        switch (event.getAction()) {
            case "CREATE":
                entityPersister.createFrom(monolithPlayer);
                break;
            case "UPDATE":
                entityPersister.updateFrom(monolithPlayer);
                break;
            case "DELETE":
                entityPersister.deleteFrom(monolithPlayer);
                break;
            default:
                throw new IllegalStateException("Unsupported action for player event handler: " + event.getAction());
        }
    }
}