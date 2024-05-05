package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PlayerControllerWithReadWriteModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_WRITE;
    }

    @Test
    public void testGetAllPlayers() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players");
        response.getBody().iterator().next().equals(testFixture.player1);
        verifyGetResponse(response, testFixture.player1, testFixture.player2, testFixture.player3, testFixture.player4);
    }

    @Test
    public void testGetPlayersByClubName() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/clubName?clubName=GS");
        verifyGetResponse(response, testFixture.player1, testFixture.player2);
    }

    @Test
    public void testGetPlayersByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/country/DE");
        verifyGetResponse(response, testFixture.player4);
    }

    @Test
    public void testGetPlayerById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<Player> response = performGetPlayerRequest("/players/" + testFixture.player1.getId());
        verifyGetResponse(response, testFixture.player1);
    }

    @Test
    public void testGetPlayersByNamePattern() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/search?name=SGS");
        verifyGetResponse(response, testFixture.player1);
    }

    @Test
    public void testCreatePlayer() {
        /*
        as there is no existing player on the service side, there will be no sync call to the monolith side
        first, player creation should only occur at the service side
        then player change event should be published as a kafka message for the monolith side to consume
         */
        Player player = new Player("BS", "TR", 100, testFixture.club2);

        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);

        //after create
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Player(savedPlayer.getId(),"BS","TR",100, new Club(123L)), "CREATE");

        Player playerFromDB = findPlayerById(savedPlayer.getId());
        verifyPlayer(new Player(savedPlayer.getId(),"BS","TR",100, testFixture.club2), playerFromDB);
        verifyPlayer(savedPlayer,playerFromDB);
        assertTrue(playerFromDB.isSynced());
    }

    @Test
    public void testUpdateRating() {
        /*
        first latest player state should be fetched from the monolith side and reflected to the service side
        then player rating update should only occur at the service side, there should be no update call to the monolith side
        finally player change event should be published as a kafka message for the monolith side to consume
         */

        registerMonolithResponse("/players/789","GET", null,200, """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "GBR",
                    "rating": 100,
                    "clubId": 456,
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);

        //before update
        Player playerFromDB = findPlayerById(testFixture.player1.getId());
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 100, testFixture.club1), playerFromDB);
        assertFalse(playerFromDB.isSynced());

        restTemplate.put("/players/" + testFixture.player1.getId() +"/rating", 200);

        //after update
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Player(789L, "SGS", "GBR", 200, testFixture.club1FromMonolith), "UPDATE");

        playerFromDB = findPlayerById(testFixture.player1.getId());
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "GBR", 200, testFixture.club1), playerFromDB);
        assertTrue(playerFromDB.isSynced());
    }

    @Test
    public void testTransferPlayer() {
        /*
        first latest player state should be fetched from the monolith side and reflected to the service side
        then player transfer should only occur at the service side, there should be no update call to the monolith side
        finally player change event should be published as a kafka message for the monolith side to consume
         */
        registerMonolithResponse("/players/789","GET", null,200, """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "GBR",
                    "rating": 100,
                    "clubId": 456,
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);

        //before update
        Player playerFromDB = findPlayerById(testFixture.player1.getId());
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 100, testFixture.club1), playerFromDB);
        assertFalse(playerFromDB.isSynced());

        restTemplate.put("/players/" + testFixture.player1.getId() + "/transfer", testFixture.club2.getId());

        //after update
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Player(789L, "SGS", "GBR", 100, new Club(123L)), "UPDATE");

        playerFromDB = findPlayerById(testFixture.player1.getId());
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "GBR", 100, testFixture.club2), playerFromDB);
        assertTrue(playerFromDB.isSynced());
    }

}
