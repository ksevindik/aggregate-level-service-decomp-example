package com.example.clubsmonolith.migration;


import com.example.clubsmonolith.model.Club;
import com.example.clubsmonolith.model.Player;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
the important point here is NOT to consume the messages whose origin is the same as the monolith itself,
and NOT to publish a change event in case we handle that event in order not to create an endless change event loop.
 */
@Component
public class EntityChangeEventListener {

    @Autowired
    private EntityPersister entityPersister;

    @Autowired
    private ObjectMapper objectMapper;

    private String targetOrigin = "service";

    @KafkaListener(topics = "entity-change-topic",groupId = "club-monolith")
    public void listen(String message) {
        try {
            EntityChangeEvent event = objectMapper.readValue(message, EntityChangeEvent.class);
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
        Club serviceClub = objectMapper.readValue(event.getEntity(), Club.class);
        switch (event.getAction()) {
            case "CREATE":
                entityPersister.createFrom(serviceClub);
                break;
            case "UPDATE":
                entityPersister.updateFrom(serviceClub);
                break;
            case "DELETE":
                entityPersister.deleteFrom(serviceClub);
                break;
            default:
                throw new IllegalStateException("Unsupported action for club event handler: " + event.getAction());
        }
    }

    private void processPlayerEvent(EntityChangeEvent event) throws JsonProcessingException {
        Player servicePlayer = objectMapper.readValue(event.getEntity(), Player.class);
        switch (event.getAction()) {
            case "CREATE":
                entityPersister.createFrom(servicePlayer);
                Player monolithPlayer;
                break;
            case "UPDATE":
                entityPersister.updateFrom(servicePlayer);
                break;
            case "DELETE":
                entityPersister.deleteFrom(servicePlayer);
                break;
            default:
                throw new IllegalStateException("Unsupported action for player event handler: " + event.getAction());
        }
    }
}