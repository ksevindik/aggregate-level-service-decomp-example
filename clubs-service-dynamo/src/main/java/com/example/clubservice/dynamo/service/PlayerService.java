package com.example.clubservice.dynamo.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.migration.EntityChangeEventPublisher;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PlayerService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    private EntityChangeEventPublisher entityChangeEventPublisher;


    public List<Player> getAllPlayers() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(#sk, :prefix)")
                .withExpressionAttributeValues(Map.of(":prefix", new AttributeValue().withS("PLAYER")))
                .withExpressionAttributeNames(Map.of("#sk", "SK"));
        return retrievePlayersWithScanning(scanExpression);
    }


    public List<Player> getPlayersByClubName(String clubName) {
        //first fetch the club with the given club name
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
                ":nameValue", new AttributeValue().withS(clubName)
        );

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("itemName = :nameValue")
                .withExpressionAttributeValues(expressionAttributeValues);
        ClubPlayerItem clubItem = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression).stream().findFirst().orElseThrow();

        //second fetch all players associated with the club
        expressionAttributeValues = Map.of(
                ":pkValue", new AttributeValue().withS(clubItem.getPK()),
                ":skPrefix", new AttributeValue().withS("PLAYER"));
        DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression<ClubPlayerItem>()
                .withKeyConditionExpression("PK = :pkValue and begins_with(SK,:skPrefix)")
                .withExpressionAttributeValues(expressionAttributeValues);
        PaginatedQueryList<ClubPlayerItem> queryList = dynamoDBMapper.query(ClubPlayerItem.class,queryExpression);
        return queryList.stream().map(ClubPlayerItem::toPlayer).toList();
    }


    public List<Player> getPlayersByCountry(String country) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("country = :value")
                .withExpressionAttributeValues(Map.of(":value", new AttributeValue().withS(country)));
        return retrievePlayersWithScanning(scanExpression);
    }


    public Optional<Player> getPlayerById(Long id) {
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
                ":skValue", new AttributeValue().withS("PLAYER#" + id));
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withIndexName("Index_SK")
                .withConsistentRead(false)
                .withFilterExpression("SK = :skValue")
                .withExpressionAttributeValues(expressionAttributeValues);
        List<Player> players = retrievePlayersWithScanning(scanExpression);
        return players.isEmpty() ? Optional.empty() : Optional.of(players.get(0));
    }


    public List<Player> getPlayersByNamePattern(String namePattern) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(SK,:skPrefix) and itemName = :value")
                .withExpressionAttributeValues(Map.of("" +
                        ":skPrefix", new AttributeValue().withS("PLAYER"),
                        ":value", new AttributeValue().withS(namePattern)));
        return retrievePlayersWithScanning(scanExpression);
    }

    public Player createPlayer(Player player) {
        player.setId(System.currentTimeMillis());
        ClubPlayerItem playerItem = ClubPlayerItem.fromPlayer(player);
        playerItem.setSynced(true);
        dynamoDBMapper.save(playerItem);
        entityChangeEventPublisher.publishClubEvent(playerItem, "CREATE");
        return player;
    }


    public Player updatePlayerRating(Long playerId, Integer rating) {
        Player player = getPlayerById(playerId).orElseThrow(()->new RuntimeException("Player not found with id :" + playerId));
        player.setRating(rating);
        ClubPlayerItem playerItem = ClubPlayerItem.fromPlayer(player);
        dynamoDBMapper.save(playerItem);
        entityChangeEventPublisher.publishClubEvent(playerItem, "UPDATE");
        return player;
    }


    public Player transferPlayer(Long playerId, Long clubId) {
        Player player = getPlayerById(playerId).orElseThrow(()->new RuntimeException("Player not found with id :" + playerId));
        ClubPlayerItem playerItem = ClubPlayerItem.fromPlayer(player);
        dynamoDBMapper.delete(playerItem);
        player.setClubId(clubId);
        playerItem = ClubPlayerItem.fromPlayer(player);
        dynamoDBMapper.save(playerItem);
        entityChangeEventPublisher.publishClubEvent(playerItem, "UPDATE");
        return player;
    }

    private List<Player> retrievePlayersWithScanning(DynamoDBScanExpression scanExpression) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        return items.stream().map(ClubPlayerItem::toPlayer).toList();
    }
}