package com.example.clubservice.dynamo.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClubService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;


    public List<Club> getAllClubs() {
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":prefix", new AttributeValue().withS("CLUB"));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("begins_with(#sk, :prefix)")
                .withExpressionAttributeValues(expressionAttributeValues)
                .withExpressionAttributeNames(new HashMap<>() {{
                    put("#sk", "SK");
                }});
        PaginatedScanList<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        return items.stream().map(ClubPlayerItem::toClub).toList();
    }

    public List<Club> getClubsByCountry(String country) {
        return List.of();
    }

    public Optional<Club> getClubById(Long id) {
        return Optional.ofNullable(null);
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        return List.of();
    }

    public Club createClub(Club club) {
        return club;
    }

    public Club updatePresident(Long clubId, String president) {
        return null;
    }
}