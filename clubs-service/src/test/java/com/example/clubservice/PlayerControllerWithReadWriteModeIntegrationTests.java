package com.example.clubservice;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PlayerControllerWithReadWriteModeIntegrationTests extends BaseIntegrationTests {

    private Club club1, club2;
    private Player player1, player2, player3, player4;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_WRITE;
    }

    @BeforeEach
    public void setUp() {
        club1 = clubRepository.save(new Club("GS", "TR", "FT"));
        club2 = clubRepository.save(new Club("BJK", "TR", "FU"));

        player1 = playerRepository.save(new Player("SGS", "TR", 100, club1));
        player2 = playerRepository.save(new Player("SYS", "TR", 90, club1));
        player3 = playerRepository.save(new Player("HS", "US", 80, club2));
        player4 = playerRepository.save(new Player("KS", "DE", 70, null));

        createIdMappings(
                new IdMapping(club1.getId(), 456L, "Club"),
                new IdMapping(club2.getId(), 123L, "Club"),
                new IdMapping(player1.getId(), 789L, "Player"));
    }


    @Test
    public void testGetAllPlayers() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(4, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player1.getId(), player2.getId(), player3.getId(), player4.getId()));
    }

    @Test
    public void testGetPlayersByClubName() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/clubName?clubName=GS",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player1.getId(), player2.getId()));
    }

    @Test
    public void testGetPlayersByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/country/DE",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player4.getId()));
    }

    @Test
    public void testGetPlayerById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<Player> response = restTemplate.getForEntity("/players/" + player1.getId(), Player.class);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(player1.getId(), response.getBody().getId());
    }

    @Test
    public void testGetPlayersByNamePattern() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side
         */
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/search?name=SGS",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player1.getId()));
    }

    @Test
    public void testCreatePlayer() {
        /*
        as there is no existing player on the service side, there will be no sync call to the monolith side
        first, player creation should only occur at the service side
        then player change event should be published as a kafka message for the monolith side to consume
         */
        Player player = new Player("BS", "TR", 100, club2);

        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);

        //after create
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Player(savedPlayer.getId(),"BS","TR",100, new Club(123L)), "CREATE");

        Player playerFromDB = findPlayerById(savedPlayer.getId());
        verifyPlayer(new Player(savedPlayer.getId(),"BS","TR",100, new Club(club2.getId())), playerFromDB);
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
        Player playerFromDB = findPlayerById(player1.getId());
        verifyPlayer(new Player(player1.getId(), "SGS", "TR", 100, new Club(club1.getId())), playerFromDB);
        assertFalse(playerFromDB.isSynced());

        restTemplate.put("/players/" + player1.getId() +"/rating", 200);

        //after update
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Player(789L, "SGS", "GBR", 200, new Club(456L)), "UPDATE");

        playerFromDB = findPlayerById(player1.getId());
        verifyPlayer(new Player(player1.getId(), "SGS", "GBR", 200, new Club(club1.getId())), playerFromDB);
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
        Player playerFromDB = findPlayerById(player1.getId());
        verifyPlayer(new Player(player1.getId(), "SGS", "TR", 100, new Club(club1.getId())), playerFromDB);
        assertFalse(playerFromDB.isSynced());

        restTemplate.put("/players/" + player1.getId() + "/transfer", club2.getId());

        //after update
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(new Player(789L, "SGS", "GBR", 100, new Club(123L)), "UPDATE");

        playerFromDB = findPlayerById(player1.getId());
        verifyPlayer(new Player(player1.getId(), "SGS", "GBR", 100, new Club(club2.getId())), playerFromDB);
        assertTrue(playerFromDB.isSynced());
    }

}
