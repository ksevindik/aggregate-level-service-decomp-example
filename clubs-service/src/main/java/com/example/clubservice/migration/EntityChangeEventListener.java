package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
the job of entity change event listener is to consume the entity change events sent from the monolith side and
delegate the processing to the entity persister.

we should only consume/process entity change events from the monolith side if the operation mode is read-only,
otherwise, we should simply discard the message
TODO: CREATE & DELETE operations in dry-run mode should also be processed in order to build id mappings on the service side

only the messages whose origin is monolith should be processed by the service, other messages with
the origin service should be ignored.
 */
@Component
public class EntityChangeEventListener {

    @Autowired
    private EntityPersister entityPersister;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationModeManager operationModeManager;

    private String targetOrigin = "monolith";

    @KafkaListener(topics = "${service.migration.entity-change-topic}",groupId = "club-service")
    public void listen(String message) {
        try {
            /*
            we should only consume/process entity change events from the monolith side if the operation mode is read-only,
            otherwise, we should simply discard the message
             */
            if(!operationModeManager.isReadOnly()) return;

            EntityChangeEvent event = objectMapper.readValue(message, EntityChangeEvent.class);
            /*
            only the messages whose origin is monolith should be processed by the service, other messages with
            the origin service should be ignored.
             */
            if (targetOrigin.equals(event.getOrigin())) {
                switch (event.getType()) {
                    case "Club":
                        processClubEvent(event);
                        break;
                    case "Player":
                        processPlayerEvent(event);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported entity type :" + event.getType());
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
                throw new IllegalArgumentException("Unsupported action for club event handler: " + event.getAction());
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
                throw new IllegalArgumentException("Unsupported action for player event handler: " + event.getAction());
        }
    }
}