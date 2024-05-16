package com.example.clubservice.dynamo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PlayerRepository {
    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public List<Player> findAll() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(#pk, :prefix)")
                .withExpressionAttributeValues(Map.of(":prefix", new AttributeValue().withS("PLAYER")))
                .withExpressionAttributeNames(Map.of("#pk", "PK"));
        return retrievePlayersWithScanning(scanExpression);
    }

    public List<Player> findByClubName(String clubName) {
        //first fetch the club with the given club name
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
                ":nameValue", new AttributeValue().withS(clubName)
        );

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("itemName = :nameValue")
                .withExpressionAttributeValues(expressionAttributeValues);
        ClubPlayerItem clubItem = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression).stream().findFirst().orElseThrow();

        //second fetch all players associated with the club
        scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(#pk,:pkPrefix) and #clubId = :clubId")
                .withExpressionAttributeValues(Map.of(
                        ":pkPrefix", new AttributeValue().withS("PLAYER"),
                        ":clubId", new AttributeValue().withN(clubItem.getId().toString())))
                .withExpressionAttributeNames(Map.of("#pk", "PK", "#clubId", "clubId"));
        return retrievePlayersWithScanning(scanExpression);
    }

    public List<Player> findByCountry(String country) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#country = :value")
                .withExpressionAttributeValues(Map.of(":value", new AttributeValue().withS(country)))
                .withExpressionAttributeNames(Map.of("#country", "country"));
        return retrievePlayersWithScanning(scanExpression);
    }

    public Optional<Player> findById(Long id) {
        ClubPlayerItem playerItem = dynamoDBMapper.load(ClubPlayerItem.class, "PLAYER#" + id, "PLAYER#" + id);
        return Optional.ofNullable(playerItem).map(ClubPlayerItem::toPlayer);
    }


    public List<Player> findByNamePattern(String namePattern) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(SK,:skPrefix) and itemName = :value")
                .withExpressionAttributeValues(Map.of("" +
                                ":skPrefix", new AttributeValue().withS("PLAYER"),
                        ":value", new AttributeValue().withS(namePattern)));
        return retrievePlayersWithScanning(scanExpression);
    }

    public Optional<Player> findByMonolithId(Long monolithId) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("monolithId = :monolithId and begins_with(SK, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":monolithId", new AttributeValue().withN(monolithId.toString()),
                        ":skPrefix", new AttributeValue().withS("PLAYER#")));
        List<Player> players = retrievePlayersWithScanning(scanExpression);
        if (players.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(players.get(0));
        }
    }

    public void save(Player player) {
        ClubPlayerItem playerItem = ClubPlayerItem.fromPlayer(player);
        dynamoDBMapper.save(playerItem);
    }

    public void delete(Player player) {
        ClubPlayerItem playerItem = ClubPlayerItem.fromPlayer(player);
        dynamoDBMapper.delete(playerItem);
    }

    private List<Player> retrievePlayersWithScanning(DynamoDBScanExpression scanExpression) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        return items.stream().map(ClubPlayerItem::toPlayer).toList();
    }
}
