package com.example.clubservice.dynamo.migration;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/*
the job of entity persister is to reflect changes received as entity change events sent from the monolith side to the
service DB directly, without triggering any business logic processing.
 */
@Component
@Transactional
public class EntityPersister {

    @Autowired
    private MonolithReadWriteApiAdapter monolithReadWriteApiAdapter;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public Club createFrom(Club monolithClub) {
        Club serviceClub = new Club();
        applyChanges(monolithClub, serviceClub);
        serviceClub.setId(System.currentTimeMillis());
        ClubPlayerItem clubPlayerItem = ClubPlayerItem.fromClub(serviceClub);
        clubPlayerItem.setMonolithId(monolithClub.getId());
        dynamoDBMapper.save(clubPlayerItem);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClub));
        return serviceClub;
    }

    private Club findClubById(Long id) {
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + id, "CLUB#" + id);
        if(item == null) {
            throw new RuntimeException("Club not found with id: " + id);
        }
        return item.toClub();
    }

    private Player findPlayerById(Long id) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, new DynamoDBScanExpression()
                .withFilterExpression("SK = :skValue")
                .withExpressionAttributeValues(Map.of(
                        ":skValue", new AttributeValue().withS("PLAYER#"+id)))
        );
        if(items.isEmpty()) {
            throw new RuntimeException("Player not found with id: " + id);
        }
        return items.get(0).toPlayer();
    }

    public Club updateFrom(Club monolithClub) {
        Long serviceClubId = resolveServiceClubId(monolithClub.getId());
        Club serviceClub = findClubById(serviceClubId);
        applyChanges(monolithClub, serviceClub);
        dynamoDBMapper.save(ClubPlayerItem.fromClub(serviceClub));
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClub));
        return serviceClub;
    }

    public void deleteFrom(Club monolithClub) {
        Long serviceClubId = resolveServiceClubId(monolithClub.getId());
        Club club = findClubById(serviceClubId);
        dynamoDBMapper.delete(ClubPlayerItem.fromClub(club));
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, club));
    }

    public Player createFrom(Player monolithPlayer) {
        Player servicePlayer = new Player();
        applyChanges(monolithPlayer, servicePlayer);
        servicePlayer.setId(System.currentTimeMillis());
        ClubPlayerItem clubPlayerItem = ClubPlayerItem.fromPlayer(servicePlayer);
        clubPlayerItem.setMonolithId(monolithPlayer.getId());
        dynamoDBMapper.save(clubPlayerItem);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayer));
        return servicePlayer;
    }

    public Player updateFrom(Player monolithPlayer) {
        Long servicePlayerId = resolveServicePlayerId(monolithPlayer.getId());
        Player servicePlayer = findPlayerById(servicePlayerId);
        applyChanges(monolithPlayer, servicePlayer);
        dynamoDBMapper.save(ClubPlayerItem.fromPlayer(servicePlayer));
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayer));
        return servicePlayer;
    }

    public void deleteFrom(Player monolithPlayer) {
        Long servicePlayerId = resolveServicePlayerId(monolithPlayer.getId());
        Player player = findPlayerById(servicePlayerId);
        dynamoDBMapper.delete(ClubPlayerItem.fromPlayer(player));
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayerId));
    }

    public void syncClubEntityIfNecessary(Long clubId) {
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + clubId, "CLUB#" + clubId);
        if(item != null && item.isSynced()) return;
        if(item == null) {
            Club club = monolithReadWriteApiAdapter.getClubById(clubId);
            item = ClubPlayerItem.fromClub(club);
        } else if(!item.isSynced()) {
            Long monolithClubId = item.getMonolithId();
            Club club = monolithReadWriteApiAdapter.getClubById(monolithClubId);
            item.applyChanges(club);
        }
        item.setSynced(true);
        dynamoDBMapper.save(item);
    }

    public void syncPlayerEntityIfNecessary(Long playerId) {
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "PLAYER#" + playerId, "PLAYER#" + playerId);
        if(item != null && item.isSynced()) return;
        if(item == null) {
            Player player = monolithReadWriteApiAdapter.getPlayerById(playerId);
            item = ClubPlayerItem.fromPlayer(player);
        } else if(!item.isSynced()) {
            Long monolithPlayerId = item.getMonolithId();
            Player player = monolithReadWriteApiAdapter.getPlayerById(monolithPlayerId);
            if(player.getClubId() != null) {
                Long clubId = resolveServiceClubId(player.getClubId());
                player.setClubId(clubId);
            }
            item.applyChanges(player);
        }
        item.setSynced(true);
        dynamoDBMapper.save(item);
    }


    private void applyChanges(Club monolithClub, Club serviceClub) {
        serviceClub.setName(monolithClub.getName());
        serviceClub.setCountry(monolithClub.getCountry());
        serviceClub.setPresident(monolithClub.getPresident());
        serviceClub.setCreated(monolithClub.getCreated());
        serviceClub.setModified(monolithClub.getModified());
    }

    private void applyChanges(Player monolithPlayer, Player servicePlayer) {
        servicePlayer.setName(monolithPlayer.getName());
        servicePlayer.setCountry(monolithPlayer.getCountry());
        servicePlayer.setRating(monolithPlayer.getRating());
        servicePlayer.setCreated(monolithPlayer.getCreated());
        servicePlayer.setModified(monolithPlayer.getModified());
        if(monolithPlayer.getClubId() != null) {
            Long serviceClubId = resolveServiceClubId(monolithPlayer.getClubId());
            servicePlayer.setClubId(serviceClubId);
        }
    }

    private Long resolveServiceClubId(Long clubId) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, new DynamoDBScanExpression()
                .withFilterExpression("monolithId = :monolithId and begins_with(SK, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":monolithId", new AttributeValue().withN(clubId.toString()),
                        ":skPrefix", new AttributeValue().withS("CLUB#")))
        );
        return items.isEmpty() ? clubId: items.get(0).getId();
    }

    private Long resolveServicePlayerId(Long playerId) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, new DynamoDBScanExpression()
                .withFilterExpression("monolithId = :monolithId and begins_with(SK, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":monolithId", new AttributeValue().withN(playerId.toString()),
                        ":skPrefix", new AttributeValue().withS("PLAYER#")))
        );
        return items.isEmpty() ? playerId: items.get(0).getId();
    }
}
