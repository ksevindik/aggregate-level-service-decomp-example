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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@TestPropertySource(properties = {
        "service.test.monolith.entity-change-event-publisher.enabled=true"
})
public class ClubControllerWithReadOnlyModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
    }

    @Test
    public void testGetAllClubs() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs/country/ES");
        verifyGetResponse(response, testFixture.club2FromMonolith);
    }

    @Test
    public void testGetClubById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<Club> response = performGetClubRequest("/clubs/" + testFixture.club1FromMonolith.getId());
        verifyGetResponse(response, testFixture.club1FromMonolith);
    }

    @Test
    public void testCreateClub() {
        /*
        new club should be created on the monolith side
        entity change event published from the monolith side should be consumed and club should be persisted on the service side
        entity ids should be from the monolith side
         */
        registerMonolithResponse("/clubs", "POST", """
                {
                    "name": "RO",
                    "country": "ES",
                    "president": "XX"
                }
                """, 201, """
                {
                    "id": 555,
                    "name": "RO",
                    "country": "ES",
                    "president": "XX"
                }
                """);

        //before create
        findByMonolithId(555L).ifPresent(c -> {
            Assertions.fail("Club should not exist before create with monolith id: 555");
        });

        Club club = new Club("RO", "ES", "XX");
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);

        //after create
        verifyClub(new Club(555L,"RO", "ES", "XX"), savedClub);

        waitForEntityPersistedEvent();

        Club clubFromDB = findByMonolithId(555L).orElseThrow(()->new IllegalStateException("Club not found with monolith id: 555"));
        verifyClub(new Club(clubFromDB.getId(),"RO", "ES", "XX"), clubFromDB);
    }

    @Test
    public void testUpdatePresident() {
        /*
        club should be updated on the monolith side
        entity change event published from the monolith side should be consumed and club should be updated on the service side
        entity ids should be from the monolith side
         */
        registerMonolithResponse("/clubs/321/president", "PUT", "AY", 200, """
                {
                    "id": 321,
                    "name": "FB",
                    "country": "TR",
                    "president": "AY",
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);

        //before update
        verifyClub(new Club(testFixture.club3.getId(),"FB", "TR", "AK"), testFixture.club3);

        Club updatedClub = restTemplate.exchange("/clubs/321/president",
                HttpMethod.PUT, new HttpEntity<String>("AY"), Club.class).getBody();

        //after update
        verifyClub(new Club(321L,"FB", "TR", "AY"), updatedClub);

        waitForEntityPersistedEvent();
        Club clubFromDB = findById(testFixture.club3.getId()).get();
        verifyClub(new Club(testFixture.club3.getId(),"FB", "TR", "AY"), clubFromDB);
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

    private Timestamp toTimestamp(String date) {
        try {
            return new Timestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(date).getTime());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
