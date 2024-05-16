package com.example.clubservice.dynamo.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ClubRepository {
    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public List<Club> findAll() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(#sk, :prefix)")
                .withExpressionAttributeValues(Map.of(":prefix", new AttributeValue().withS("CLUB")))
                .withExpressionAttributeNames(Map.of("#sk", "SK"));
        return retrieveClubsWithScanning(scanExpression);
    }

    public List<Club> findByCountry(String country) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("country = :value")
                .withExpressionAttributeValues(Map.of(":value", new AttributeValue().withS(country)));
        return retrieveClubsWithScanning(scanExpression);
    }

    public Optional<Club> findById(Long id) {
        ClubPlayerItem clubPlayerItem = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + id, "CLUB#" + id);
        if (clubPlayerItem != null) {
            return Optional.of(clubPlayerItem.toClub());
        } else {
            return Optional.empty();
        }
    }

    public List<Club> findByNamePattern(String namePattern) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return retrieveClubsWithScanning(scanExpression).stream().filter(club -> club.getName().contains(namePattern)).toList();
    }

    public Optional<Club> findByMonolithId(Long monolithId) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("monolithId = :monolithId and begins_with(SK, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":monolithId", new AttributeValue().withN(monolithId.toString()),
                        ":skPrefix", new AttributeValue().withS("CLUB#")));
        List<Club> clubs = retrieveClubsWithScanning(scanExpression);
        if (clubs.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(clubs.get(0));
        }
    }

    public void save(Club club) {
        ClubPlayerItem clubPlayerItem = ClubPlayerItem.fromClub(club);
        dynamoDBMapper.save(clubPlayerItem);
    }

    public void delete(Club club) {
        ClubPlayerItem clubPlayerItem = ClubPlayerItem.fromClub(club);
        dynamoDBMapper.delete(clubPlayerItem);
    }

    private List<Club> retrieveClubsWithScanning(DynamoDBScanExpression scanExpression) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        return items.stream().map(ClubPlayerItem::toClub).toList();
    }
}
