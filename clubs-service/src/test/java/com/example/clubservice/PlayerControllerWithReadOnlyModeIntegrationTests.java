package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNull;


@TestPropertySource(properties = {
        "monolith.entity-change-event-publisher.enabled=true"
})
public class PlayerControllerWithReadOnlyModeIntegrationTests extends BaseOperationModeIntegrationTests {
    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
    }

    @Test
    public void testGetAllPlayers() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players");

        verifyGetResponse(response,
                testFixture.player1FromMonolith,
                testFixture.player2FromMonolith,
                testFixture.player3FromMonolith,
                testFixture.player4FromMonolith
        );
    }

    @Test
    public void testGetPlayersByClubName() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/clubName?clubName=GS");
        verifyGetResponse(response,
                testFixture.player1FromMonolith,
                testFixture.player2FromMonolith
        );
    }

    @Test
    public void testGetPlayersByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/country/DE");
        verifyGetResponse(response, testFixture.player4FromMonolith);
    }

    @Test
    public void testGetPlayerById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<Player> response = performGetPlayerRequest("/players/789");
        verifyGetResponse(response, testFixture.player1FromMonolith);
    }

    @Test
    public void testGetPlayersByNamePattern() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Player>> response = performGetPlayersRequest("/players/search?name=SGS");
        verifyGetResponse(response, testFixture.player1FromMonolith);
    }

    @Test
    public void testCreatePlayer() {
        /*
        new player should be created on the monolith side
        entity change event published from the monolith side should be consumed and player should be persisted on the service side
        entity ids should be from the monolith side
         */
        Player player = new Player("BS", "TR", 100, new Club(456L));

        registerMonolithResponse("/players", "POST", """
                {
                    "name": "BS",
                    "country": "TR",
                    "rating": 100,
                    "clubId": 456
                }""",201,
                """
                {
                    "id": 123,
                    "name": "BS",
                    "country": "TR",
                    "rating": 100,
                    "clubId": 456
                }""");

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(123L, "Player");
        assertNull(idMapping);

        Player savedPlayer = restTemplate.postForObject("/players", player,Player.class);

        waitForEntityPersistedEvent();

        verifyPlayer(new Player(123L, "BS", "TR", 100, new Club(456L)), savedPlayer);
        idMapping = idMappingRepository.findByMonolithIdAndTypeName(123L, "Player");
        verifyPlayer(new Player(idMapping.getServiceId(), "BS", "TR", 100, testFixture.club1), playerRepository.findById(idMapping.getServiceId()).get());
    }

    @Test
    public void testUpdateRating() {
        /*
        player rating should be updated on the monolith side
        entity change event published from the monolith side should be consumed and player should be updated on the service side
        entity ids should be from the monolith side
         */

        registerMonolithResponse("/players/789/rating", "PUT", "200",200,
                """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "TR",
                    "rating": 200,
                    "clubId": 456,
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }""" );

        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 100, testFixture.club1),
                playerRepository.findById(testFixture.player1.getId()).get());

        Player updatedPlayer = restTemplate.exchange(
                "/players/789/rating",
                HttpMethod.PUT, new HttpEntity<Integer>(200),Player.class).getBody();

        waitForEntityPersistedEvent();

        verifyPlayer(new Player(789L, "SGS", "TR", 200, new Club(456L)), updatedPlayer);
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 200, testFixture.club1),
                playerRepository.findById(testFixture.player1.getId()).get());
    }

    @Test
    public void testTransferPlayer() {
        /*
        player transfer should occur on the monolith side
        entity change event published from the monolith side should be consumed and player should be updated on the service side
        entity ids should be from the monolith side
         */

        registerMonolithResponse("/players/789/transfer", "PUT", "123",200,
                """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "TR",
                    "rating": 100,
                    "clubId": 123,
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }""");

        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 100, testFixture.club1),
                playerRepository.findById(testFixture.player1.getId()).get());

        Player updatedPlayer = restTemplate.exchange(
                "/players/789/transfer",
                HttpMethod.PUT, new HttpEntity<Long>(123L),Player.class).getBody();

        waitForEntityPersistedEvent();

        verifyPlayer(new Player(789L, "SGS", "TR", 100, new Club(123L)), updatedPlayer);
        verifyPlayer(new Player(testFixture.player1.getId(), "SGS", "TR", 100, testFixture.club2),
                playerRepository.findById(testFixture.player1.getId()).get());
    }

}
