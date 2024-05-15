package com.example.clubservice.dynamo;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.clubservice.dynamo.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.dynamo.migration.OperationMode;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClubControllerWithReadWriteModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_WRITE;
    }

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Test
    public void testGetAllClubs() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs");
        verifyGetResponse(response, testFixture.club1, testFixture.club2, testFixture.club3);
    }

    @Test
    public void testGetClubsByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs/country/ES");
        verifyGetResponse(response, testFixture.club2);
    }

    @Test
    public void testGetClubById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<Club> response = performGetClubRequest("/clubs/" + testFixture.club1.getId());
        verifyGetResponse(response, testFixture.club1);
    }

    @Test
    public void testCreateClub() throws InterruptedException {
        /*
        as there is no existing club on the service side, there will be no sync call to the monolith side
        first, club creation should only occur at the service side
        then club change event should be published from the service side as a kafka message to be consumed by the monolith side
         */
        Club club = new Club("RW", "ES", "XX");
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);

        //after create
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Club(savedClub.getId(), "RW", "ES", "XX"), "CREATE");

        Club clubFromDB = this.findById(savedClub.getId()).orElseThrow();

        verifyClub(new Club(savedClub.getId(), "RW", "ES", "XX"), savedClub);
        verifyClub(clubFromDB, savedClub);
        assertTrue(clubFromDB.isSynced());
    }



    @Test
    public void testUpdatePresident() {
        /*
        first latest club state should be fetched from the monolith side and reflected to the service side
        then club update should only occur at the service side, there should be no update call to the monolith side
        finally club change event should be published from the service side as a kafka message to be consumed by the monolith side
         */
        registerMonolithResponse("/clubs/456","GET",null,200,"""
                {
                    "id": 456,
                    "name": "GS",
                    "country": "TRY",
                    "president": "FT",
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);

        //before update
        Club clubFromDB = this.findById(testFixture.club1.getId()).orElseThrow();
        verifyClub(new Club(testFixture.club1.getId(),"GS","TR","FT"), clubFromDB);
        assertFalse(clubFromDB.isSynced());

        restTemplate.put("/clubs/"+ testFixture.club1.getId() + "/president", "AY");

        //after update
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Club(456L,"GS","TRY","AY",
                toTimestamp("2021-07-01T00:00:00"),
                toTimestamp("2021-07-01T00:00:00")), "UPDATE");

        clubFromDB = this.findById(testFixture.club1.getId()).orElseThrow();
        verifyClub(new Club(testFixture.club1.getId(), "GS", "TRY", "AY"), clubFromDB);
        assertTrue(clubFromDB.isSynced());
    }
}
