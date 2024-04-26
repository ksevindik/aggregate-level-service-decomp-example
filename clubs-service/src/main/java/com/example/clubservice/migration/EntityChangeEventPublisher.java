package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.IdMappingRepository;
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

    @Autowired
    private IdMappingRepository idMappingRepository;

    @Autowired
    private OperationModeManager operationModeManager;

    private String sourceOrigin = "service";

    public void publishClubEvent(Club club, String action) {
        IdMapping idMapping = idMappingRepository.findByServiceIdAndTypeName(club.getId(), "Club");
        if(idMapping != null) {
            club.setId(idMapping.getMonolithId());
        }
        this.publish(action, club, club.getId());
    }

    public void publishPlayerEvent(Player player, String action) {
        IdMapping idMappingForPlayer = idMappingRepository.findByServiceIdAndTypeName(player.getId(), "Player");
        if(idMappingForPlayer != null) {
            player.setId(idMappingForPlayer.getMonolithId());
        }
        if(player.getClubId() != null) {
            IdMapping idMappingForClub = idMappingRepository.findByServiceIdAndTypeName(player.getClubId(), "Club");
            if(idMappingForClub != null) {
                player.setClubId(idMappingForClub.getMonolithId());
            }
        }
        this.publish(action, player, player.getClubId()!=null ? player.getClubId() : player.getId());
    }

    private void publish(String action, Object entity, Long key) {
        //we will not publish any event in read-only or dry-run modes
        if(!operationModeManager.isReadWrite()) return;
        try {
            String entityState = objectMapper.writeValueAsString(entity);
            EntityChangeEvent event = new EntityChangeEvent(action, entity.getClass().getSimpleName(), sourceOrigin, entityState);
            String message = objectMapper.writeValueAsString(event);
            System.out.println(">>>Publishing entity change event: " + message);
            kafkaTemplate.send("entity-change-topic", key.toString(), message).get();
        } catch (Exception e) {
            // Handle error, could log or throw a custom unchecked exception
            throw new RuntimeException("Failed to publish entity change event", e);
        }
    }
}