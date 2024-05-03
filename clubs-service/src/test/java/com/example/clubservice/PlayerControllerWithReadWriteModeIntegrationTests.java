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
        Player player = new Player("BS", "TR", 100, club2);

        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);
        Player playerFromDB = playerRepository.findById(savedPlayer.getId()).orElseThrow();

        assertEquals(savedPlayer.getId(), playerFromDB.getId());

        assertEquals("BS", savedPlayer.getName());
        assertEquals("TR", savedPlayer.getCountry());
        assertEquals(100, savedPlayer.getRating());
        assertEquals(club2.getId(), savedPlayer.getClubId());

        assertEquals("BS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(100, playerFromDB.getRating());
        assertEquals(club2.getId(), playerFromDB.getClubId());
    }

    @Test
    public void testUpdateRating() {
        idMappingRepository.save(new IdMapping(club1.getId(), 456L, "Club"));
        idMappingRepository.save(new IdMapping(player1.getId(), 789L, "Player"));

        String responseBodyForGetPlayerById = """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "GBR",
                    "rating": 100,
                    "clubId": 456,
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """;
        WireMock.stubFor(WireMock.get("/players/789")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetPlayerById)));

        restTemplate.put("/players/" + player1.getId() +"/rating", 200);
        Player playerFromDB = playerRepository.findById(player1.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("GBR", playerFromDB.getCountry());
        assertEquals(200, playerFromDB.getRating());
        assertEquals(club1.getId(), playerFromDB.getClubId());
    }

    @Test
    public void testTransferPlayer() {
        idMappingRepository.save(new IdMapping(club1.getId(), 456L, "Club"));
        idMappingRepository.save(new IdMapping(club2.getId(), 123L, "Club"));
        idMappingRepository.save(new IdMapping(player1.getId(), 789L, "Player"));

        String responseBodyForGetPlayerById = """
                {
                    "id": 789,
                    "name": "SGS",
                    "country": "GBR",
                    "rating": 100,
                    "clubId": 456,
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """;
        WireMock.stubFor(WireMock.get("/players/789")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetPlayerById)));


        restTemplate.put("/players/" + player1.getId() + "/transfer", club2.getId());
        Player playerFromDB = playerRepository.findById(player1.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("GBR", playerFromDB.getCountry());
        assertEquals(100, playerFromDB.getRating());
        assertEquals(club2.getId(), playerFromDB.getClubId());
    }

}
