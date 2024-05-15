package com.example.clubservice.dynamo;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.dynamo.migration.OperationMode;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class PlayerControllerWithDryRunModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.DRY_RUN;
    }

    @Test
    public void testGetAllPlayers() {
        /*
        players should be retrieved from the monolith side
         */
        registerMonolithResponse("/players", "GET", null, 200, """
                [
                    {
                        "id": 789,
                        "name": "SGS",
                        "country": "TR",
                        "rating": 100,
                        "clubId": 456
                    },
                    {
                        "id": 790,
                        "name": "SYS",
                        "country": "TR",
                        "rating": 90,
                        "clubId": 456
                    },
                    {
                        "id": 780,
                        "name": "HS",
                        "country": "US",
                        "rating": 80,
                        "clubId": 123
                    },
                    {
                        "id": 770,
                        "name": "KS",
                        "country": "DE",
                        "rating": 70
                    }
                ]
                """);

        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players");

        verifyGetResponse(response,
                testFixture.player1FromMonolith,
                testFixture.player2FromMonolith,
                testFixture.player3FromMonolith,
                testFixture.player4FromMonolith);
    }

    @Test
    public void testGetPlayersByClubName() {
        /*
        players should be retrieved from the monolith side
         */
        registerMonolithResponse("/players/clubName?clubName=GS", "GET", null, 200, """
                [
                    {
                        "id": 789,
                        "name": "SGS",
                        "country": "TR",
                        "rating": 100,
                        "clubId": 456
                    },
                    {
                        "id": 790,
                        "name": "SYS",
                        "country": "TR",
                        "rating": 90,
                        "clubId": 456
                    }
                ]
                """);
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/clubName?clubName=GS");
        verifyGetResponse(response,
                testFixture.player1FromMonolith,
                testFixture.player2FromMonolith)
        ;
    }

    @Test
    public void testGetPlayersByCountry() {
        /*
        players should be retrieved from the monolith side
         */
        registerMonolithResponse("/players/country/DE", "GET", null, 200, """
                [
                    {
                        "id": 770,
                        "name": "KS",
                        "country": "DE",
                        "rating": 70
                    }
                ]
                """);
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/country/DE");

        verifyGetResponse(response, testFixture.player4FromMonolith);
    }

    @Test
    public void testGetPlayerById() {
        /*
        players should be retrieved from the monolith side
         */
        registerMonolithResponse("/players/789", "GET", null, 200, """
                    {
                        "id": 789,
                        "name": "SGS",
                        "country": "TR",
                        "rating": 100,
                        "clubId": 456
                    }
                """);
        ResponseEntity<Player> response = performGetPlayerRequest("/players/789");
        verifyGetResponse(response, testFixture.player1FromMonolith);
    }

    @Test
    public void testGetPlayersByNamePattern() {
        /*
        players should be retrieved from the monolith side
         */
        registerMonolithResponse("/players/search?name=SGS", "GET", null, 200, """
                [
                    {
                        "id": 789,
                        "name": "SGS",
                        "country": "TR",
                        "rating": 100,
                        "clubId": 456
                    }
                ]
                """);
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/search?name=SGS");
        verifyGetResponse(response, testFixture.player1FromMonolith);
    }

    @Test
    public void testCreatePlayer() {
        /*
        player should be created on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */

        registerMonolithResponse("/players", "POST", """
                {
                    "name": "DR",
                    "country": "TR",
                    "rating": 99,
                    "clubId": 456
                }
                """, 201, """
                {
                    "id": 111,
                    "name": "DR",
                    "country": "TR",
                    "rating": 99,
                    "clubId": 456
                }
                """);

        findByMonolithId(111L).ifPresent(c -> {
            throw new IllegalStateException("Player should not exist before create with monolith id: 111");
        });
        Player player = new Player("DR", "TR", 99, testFixture.club1FromMonolith.getId());
        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);
        verifyPlayer(new Player(111L, "DR", "TR", 99, testFixture.club1FromMonolith.getId()), savedPlayer);

        Player playerFromDB = findPlayerByMonolithId(111L).orElseThrow(()->new IllegalStateException("Player not found with monolith id: " + 111L));
        verifyPlayer(new Player(playerFromDB.getId(), "DR", "TR", 99, testFixture.club1.getId()), playerFromDB);
    }

    @Test
    public void testUpdateRating() {
        /*
        player should be updated on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */
        registerMonolithResponse("/players/789/rating", "PUT", "200", 200, """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "TR",
                    "rating": 200,
                    "clubId": 456
                }
                """);
        restTemplate.put("/players/789/rating", 200);
        Player playerFromDB = findPlayerById(testFixture.player1.getId());
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 200, testFixture.club1.getId()), playerFromDB);
    }

    @Test
    public void testTransferPlayer() {
        /*
        player should be updated on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */
        registerMonolithResponse("/players/789/transfer", "PUT", "123", 200, """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "TR",
                    "rating": 100,
                    "clubId": 123
                }
                """);

        restTemplate.put("/players/789/transfer", 123);
        Player playerFromDB = findPlayerById(testFixture.player1.getId());
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 100, testFixture.club2.getId()), playerFromDB);
    }
}
