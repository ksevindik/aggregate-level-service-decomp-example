package com.example.clubservice.dynamo.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClubService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;


    public List<Club> getAllClubs() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(#sk, :prefix)")
                .withExpressionAttributeValues(Map.of(":prefix", new AttributeValue().withS("CLUB")))
                .withExpressionAttributeNames(Map.of("#sk", "SK"));
        return retrieveClubsWithScanning(scanExpression);
    }

    public List<Club> getClubsByCountry(String country) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("country = :value")
                .withExpressionAttributeValues(Map.of(":value", new AttributeValue().withS(country)));
        return retrieveClubsWithScanning(scanExpression);
    }

    public Optional<Club> getClubById(Long id) {
        ClubPlayerItem clubPlayerItem = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + id, "CLUB#" + id);
        if (clubPlayerItem != null) {
            return Optional.of(clubPlayerItem.toClub());
        } else {
            return Optional.empty();
        }
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return retrieveClubsWithScanning(scanExpression).stream().filter(club -> club.getName().contains(namePattern)).toList();
    }

    public Club createClub(Club club) {
        club.setId(System.currentTimeMillis());
        dynamoDBMapper.save(ClubPlayerItem.fromClub(club));
        return club;
    }

    public Club updatePresident(Long clubId, String president) {
        ClubPlayerItem clubPlayerItem = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + clubId, "CLUB#" + clubId);
        if (clubPlayerItem != null) {
            clubPlayerItem.setPresident(president);
            dynamoDBMapper.save(clubPlayerItem);
            return clubPlayerItem.toClub();
        } else {
            throw new RuntimeException("Club not found with id :" + clubId);
        }
    }

    private List<Club> retrieveClubsWithScanning(DynamoDBScanExpression scanExpression) {
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        return items.stream().map(ClubPlayerItem::toClub).toList();
    }
}