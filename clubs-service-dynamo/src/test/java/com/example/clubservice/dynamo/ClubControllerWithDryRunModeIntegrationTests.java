package com.example.clubservice.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.dynamo.migration.OperationMode;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClubControllerWithDryRunModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.DRY_RUN;
    }

    @Test
    public void testGetAllClubs() {
        /*
        clubs should be retrieved from the monolith side
         */
        registerMonolithResponse("/clubs", "GET", null, 200, """
                [
                    {
                        "id": 456,
                        "name": "GS",
                        "country": "TR",
                        "president": "FT"
                    },
                    {
                        "id": 123,
                        "name": "BJK",
                        "country": "ES",
                        "president": "FU"
                    },
                    {
                        "id": 321,
                        "name": "FB",
                        "country": "TR",
                        "president": "AK"
                    }
                ]
                """);
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        /*
        clubs should be retrieved from the monolith side
         */
        registerMonolithResponse("/clubs/country/TR", "GET", null, 200, """
                [
                    {
                        "id": 456,
                        "name": "GS",
                        "country": "TR",
                        "president": "FT"
                    },
                    {
                        "id": 321,
                        "name": "FB",
                        "country": "TR",
                        "president": "AK"
                    }
                ]
                """);
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs/country/TR");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubById() {
        /*
        clubs should be retrieved from the monolith side
         */
        registerMonolithResponse("/clubs/123", "GET", null, 200, """
                    {
                        "id": 123,
                        "name": "BJK",
                        "country": "ES",
                        "president": "FU"
                    }
                """);
        ResponseEntity<Club> response = performGetClubRequest("/clubs/123");
        verifyGetResponse(response, testFixture.club2FromMonolith);
    }

    @Test
    public void testCreateClub() {
        /*
        club should be created on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */
        registerMonolithResponse("/clubs", "POST", """
                {
                    "name": "DR",
                    "country": "ES",
                    "president": "XX"
                }
                """, 201, """
                {
                    "id": 444,
                    "name": "DR",
                    "country": "ES",
                    "president": "XX"
                }
                """);

        findByMonolithId(444L).ifPresent(c -> {
            Assertions.fail("Club should not exist before create with monolith id: 444");
        });

        Club club = new Club("DR", "ES", "XX");
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        verifyClub(new Club(444L,"DR", "ES", "XX"), savedClub);

        Club clubFromDB = findByMonolithId(555L).orElseThrow(()->new IllegalStateException("Club not found with monolith id: 444"));
        verifyClub(new Club(clubFromDB.getId(),"DR", "ES", "XX"), clubFromDB);
    }

    @Test
    public void testUpdatePresident() {
        /*
        club should be updated on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */
        registerMonolithResponse("/clubs/321/president", "PUT", "AY", 200, """
                {
                    "id": 321,
                    "name": "FB",
                    "country": "TR",
                    "president": "AK"
                }
                """);
        restTemplate.put("/clubs/321/president", "AY");

        Club clubFromDB = findById(testFixture.club3.getId()).orElseThrow();
        verifyClub(new Club(testFixture.club3.getId(), "FB", "TR", "AY"), clubFromDB);
    }

    private Optional<Club> findById(Long id) {
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + id, "CLUB#" + id);
        if(item == null) {
            return Optional.empty();
        }
        return Optional.of(item.toClub());
    }

    private Optional<Club> findByMonolithId(Long id) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.withFilterExpression("#monolithId = :val1 and begins_with(#sk, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":val1", new AttributeValue().withN(id.toString()),
                        ":skPrefix", new AttributeValue().withS("CLUB#")))
                .withExpressionAttributeNames(Map.of("#sk", "SK", "#monolithId", "monolithId"));
        List<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        if(items.isEmpty()) {
            return Optional.empty();
        } else if(items.size()>1) {
            throw new IllegalStateException("Multiple clubs found with monolith id: " + id);
        }
        return Optional.of(items.get(0).toClub());
    }
}
