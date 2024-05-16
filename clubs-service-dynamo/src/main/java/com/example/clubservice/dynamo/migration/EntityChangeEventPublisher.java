package com.example.clubservice.dynamo.migration;

import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.Player;
import com.example.clubservice.dynamo.repository.ClubRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/*
the job of entity change event publisher is to publish the entity change events to the kafka topic so that
the monolith side could handle them. entity change events are published through business logic processing.
 */
@Component
public class EntityChangeEventPublisher {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationModeManager operationModeManager;

    @Autowired
    private MigrationProperties migrationProperties;

    @Autowired
    private ClubRepository clubRepository;

    public void publishClubEvent(Club club, String action) {
        /*
         we should translate id of the entity to monolith id before publishing the event
         however, we need to create a copy of the entity in order to avoid any changes to the original entity
         */
        club = new Club(club);
        if(!action.equals("CREATE")) {
            club.setId(club.getMonolithId());
        }
        this.publish(action, club, club.getId());
    }

    public void publishPlayerEvent(Player player, String action) {
        /*
         we should translate id of the entity to monolith id before publishing the event
         however, we need to create a copy of the entity in order to avoid any changes to the original entity
         */
        player = new Player(player);
        if(!action.equals("CREATE")) {
            player.setId(player.getMonolithId());
        }
        Long clubId = player.getClubId();
        if(clubId != null) {
            Club club = clubRepository.findById(clubId).orElseThrow(() ->
                    new RuntimeException("Club not found with id: " + clubId));
            Long clubMonolithId = club.getMonolithId();
            player.setClubId(clubMonolithId!=null?clubMonolithId:clubId);
        }
        this.publish(action, player, clubId!=null ? clubId : player.getId());
    }

    private void publish(String action, Object entity, Long key) {
        /*
        we should only publish change events from the service side if the operation mode is read-write
         */
        if(!operationModeManager.isReadWrite()) return;
        try {
            String entityState = objectMapper.writeValueAsString(entity);
            EntityChangeEvent event = new EntityChangeEvent(
                    action,
                    entity.getClass().getSimpleName(),
                    migrationProperties.getSourceOrigin(),
                    entityState);
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(migrationProperties.getEntityChangeTopic(), key.toString(), message).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish entity change event", e);
        }
    }
}