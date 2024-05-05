package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


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
        Player player = new Player("XXX", "TR", 99, null);
        player.setClubId(456L);

        WireMock.stubFor(WireMock.post("/players")
                .willReturn(WireMock.aResponse().withStatus(201)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 111,
                                    "name": "XXX",
                                    "country": "TR",
                                    "rating": 99,
                                    "clubId": 456
                                }
                                """)));

        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);

        assertEquals(111L, savedPlayer.getId());
        assertEquals("XXX", savedPlayer.getName());
        assertEquals("TR", savedPlayer.getCountry());
        assertEquals(99, savedPlayer.getRating());
        assertEquals(456L, savedPlayer.getClubId());

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(111L, "Player");

        Player playerFromDB = playerRepository.findById(idMapping.getServiceId()).orElseThrow();
        assertEquals("XXX", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(99, playerFromDB.getRating());
        assertEquals(testFixture.club1.getId(), playerFromDB.getClubId());
    }

    @Test
    public void testUpdateRating() {
        WireMock.stubFor(WireMock.put("/players/789/rating").withRequestBody(WireMock.equalTo("200"))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 789,
                                    "name": "SGS",
                                    "country": "TR",
                                    "rating": 200,
                                    "clubId": 456
                                }
                                """)));
        restTemplate.put("/players/789/rating", 200);
        Player playerFromDB = playerRepository.findById(testFixture.player1.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(200, playerFromDB.getRating());
        assertEquals(testFixture.club1.getId(), playerFromDB.getClubId());
    }

    @Test
    public void testTransferPlayer() {
        WireMock.stubFor(WireMock.put("/players/789/transfer").withRequestBody(WireMock.equalTo("123"))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 789,
                                    "name": "SGS",
                                    "country": "TR",
                                    "rating": 100,
                                    "clubId": 123
                                }
                                """)));

        restTemplate.put("/players/789/transfer", 123);
        Player playerFromDB = playerRepository.findById(testFixture.player1.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(100, playerFromDB.getRating());
        assertEquals(testFixture.club2.getId(), playerFromDB.getClubId());
    }

}
