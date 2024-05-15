package com.example.clubservice.dynamo.migration;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
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
    private DynamoDBMapper dynamoDBMapper;

    public void publishClubEvent(Club club, String action) {
        /*
         we should translate id of the entity to monolith id before publishing the event
         however, we need to create a copy of the entity in order to avoid any changes to the original entity
         */
        club = new Club(club);
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + club.getId(), "CLUB#" + club.getId());
        Long monolithId = item.getMonolithId();
        if(monolithId != null) {
            club.setId(monolithId);
        }
        this.publish(action, club, club.getId());
    }

    public void publishPlayerEvent(Player player, String action) {
        /*
         we should translate id of the entity to monolith id before publishing the event
         however, we need to create a copy of the entity in order to avoid any changes to the original entity
         */
        player = new Player(player);
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "PLAYER#" + player.getId(), "PLAYER#" + player.getId());
        Long monolithId = item.getMonolithId();
        if(monolithId != null) {
            player.setId(monolithId);
        }
        if(player.getClubId() != null) {
            ClubPlayerItem clubItem = dynamoDBMapper.load(ClubPlayerItem.class,
                    "CLUB#" + player.getClubId(), "CLUB#" + player.getClubId());
            player.setClubId(clubItem.getMonolithId());
        }
        this.publish(action, player, player.getClubId()!=null ? player.getClubId() : player.getId());
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