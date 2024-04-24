package com.example.clubsmonolith.migration;

import com.example.clubsmonolith.model.Club;
import com.example.clubsmonolith.model.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EntityChangeEventPublisher {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void publishClubEvent(Club club, String action) {
        this.publish(action, club, club.getId());
    }

    public void publishPlayerEvent(Player player, String action) {
        this.publish(action, player, player.getClubId()!=null ? player.getClubId() : player.getId());
    }

    private void publish(String action, Object entity, Long key) {
        try {
            String entityState = objectMapper.writeValueAsString(entity);
            EntityChangeEvent event = new EntityChangeEvent(action, entity.getClass().getSimpleName(), "monolith", entityState);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("entity-change-topic", key.toString(), message).get();
        } catch (Exception e) {
            // Handle error, could log or throw a custom unchecked exception
            throw new RuntimeException("Failed to publish entity change event", e);
        }
    }
}