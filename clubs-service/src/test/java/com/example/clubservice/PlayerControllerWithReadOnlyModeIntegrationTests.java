package com.example.clubservice;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@TestPropertySource(properties = {
        "monolith.entity-change-event-publisher.enabled=true"
})
public class PlayerControllerWithReadOnlyModeIntegrationTests extends BaseIntegrationTests {

    private Club club1, club2;
    private Player player1, player2, player3, player4;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
    }

    @BeforeEach
    public void setUp() {
        club1 = clubRepository.save(new Club("GS", "TR", "FT"));
        club2 = clubRepository.save(new Club("BJK", "TR", "FU"));

        player1 = playerRepository.save(new Player("SGS", "TR", 100, club1));
        player2 = playerRepository.save(new Player("SYS", "TR", 90, club1));
        player3 = playerRepository.save(new Player("HS", "US", 80, club2));
        player4 = playerRepository.save(new Player("KS", "DE", 70, null));
    }


    @Test
    public void testGetAllPlayers() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(4, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player1.getId(), player2.getId(), player3.getId(), player4.getId()));
    }

    @Test
    public void testGetPlayersByClubName() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/clubName?clubName=GS",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player1.getId(), player2.getId()));
    }

    @Test
    public void testGetPlayersByCountry() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/country/DE",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player4.getId()));
    }

    @Test
    public void testGetPlayerById() {
        ResponseEntity<Player> response = restTemplate.getForEntity("/players/" + player1.getId(), Player.class);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(player1.getId(), response.getBody().getId());
    }

    @Test
    public void testGetPlayersByNamePattern() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/search?name=SGS",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(player1.getId()));
    }

    @Test
    public void testCreatePlayer() {
        Player player = new Player("BS", "TR", 100, new Club(456L));

        createIdMappings(new IdMapping(club2.getId(), 456L, "Club"));

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
        verifyPlayer(new Player(idMapping.getServiceId(), "BS", "TR", 100, new Club(club2.getId())),
                playerRepository.findById(idMapping.getServiceId()).get());

    }

    @Test
    public void testUpdateRating() {
        createIdMappings(
                new IdMapping(club1.getId(), 456L, "Club"),
                new IdMapping(player1.getId(), 789L, "Player"));

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

        verifyPlayer(new Player(player1.getId(), "SGS", "TR", 100, new Club(club1.getId())),
                playerRepository.findById(player1.getId()).get());

        Player updatedPlayer = restTemplate.exchange(
                "/players/789/rating",
                HttpMethod.PUT, new HttpEntity<Integer>(200),Player.class).getBody();

        waitForEntityPersistedEvent();

        verifyPlayer(new Player(789L, "SGS", "TR", 200, new Club(456L)), updatedPlayer);
        verifyPlayer(new Player(player1.getId(), "SGS", "TR", 200, new Club(club1.getId())),
                playerRepository.findById(player1.getId()).get());
    }

    @Test
    public void testTransferPlayer() {
        createIdMappings(
                new IdMapping(club1.getId(), 456L, "Club"),
                new IdMapping(club2.getId(), 123L, "Club"),
                new IdMapping(player1.getId(), 789L, "Player"));

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

        verifyPlayer(new Player(player1.getId(), "SGS", "TR", 100, new Club(club1.getId())),
                playerRepository.findById(player1.getId()).get());

        Player updatedPlayer = restTemplate.exchange(
                "/players/789/transfer",
                HttpMethod.PUT, new HttpEntity<Long>(123L),Player.class).getBody();

        waitForEntityPersistedEvent();

        verifyPlayer(new Player(789L, "SGS", "TR", 100, new Club(123L)), updatedPlayer);
        verifyPlayer(new Player(player1.getId(), "SGS", "TR", 100, new Club(club2.getId())),
                playerRepository.findById(player1.getId()).get());
    }

}
