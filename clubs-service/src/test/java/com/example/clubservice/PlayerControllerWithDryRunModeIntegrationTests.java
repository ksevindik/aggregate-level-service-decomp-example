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


public class PlayerControllerWithDryRunModeIntegrationTests extends BaseIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.DRY_RUN;
    }

    @BeforeEach
    public void setUp() {
        String responseBodyForGetAllPlayers = """
                [
                    {
                        "id": 123
                    },
                    {
                        "id": 456
                    },
                    {
                        "id": 789
                    }
                ]
                """;

        String responseBodyForGetPlayersByClubName = """
                [
                    {
                        "id": 123
                    }
                ]
                """;

        String responseBodyForGetPlayersByCountry = """
                [
                    {
                        "id": 123
                    },
                    {
                        "id": 456
                    }
                ]
                """;

        String responseBodyForGetPlayerById = """
                {
                    "id": 123
                }
                """;

        String responseBodyForGetPlayersByNamePattern = """
                [
                    {
                        "id": 456
                    },
                    {
                        "id": 789
                    }
                ]
                """;


        WireMock.stubFor(WireMock.get("/players")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetAllPlayers)));

        WireMock.stubFor(WireMock.get("/players/clubName?clubName=FB")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetPlayersByClubName)));

        WireMock.stubFor(WireMock.get("/players/country/TR")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetPlayersByCountry)));

        WireMock.stubFor(WireMock.get("/players/123")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetPlayerById)));

        WireMock.stubFor(WireMock.get("/players/search?name=SGS")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetPlayersByNamePattern)));
    }

    @Test
    public void testGetAllPlayers() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(3, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(123L, 456L, 789L));
    }

    @Test
    public void testGetPlayersByClubName() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/clubName?clubName=FB",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(123L, response.getBody().get(0).getId());
    }

    @Test
    public void testGetPlayersByCountry() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/country/TR",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(123L, 456L));
    }

    @Test
    public void testGetPlayerById() {
        ResponseEntity<Player> response = restTemplate.getForEntity("/players/123", Player.class);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(123L, response.getBody().getId());
    }

    @Test
    public void testGetPlayersByNamePattern() {
        ResponseEntity<List<Player>> response = restTemplate.exchange("/players/search?name=SGS",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(p->p.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(456L, 789L));
    }

    @Test
    public void testCreatePlayer() {
        Club club = clubRepository.save(new Club("FB", "TR", "AK"));

        idMappingRepository.save(new IdMapping(club.getId(), 456L, "Club"));

        Player player = new Player("SGS", "TR", 100, null);
        player.setClubId(456L);

        WireMock.stubFor(WireMock.post("/players")
                .willReturn(WireMock.aResponse().withStatus(201)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 123,
                                    "name": "SGS",
                                    "country": "TR",
                                    "rating": 100,
                                    "clubId": 456
                                }
                                """)));

        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);

        assertEquals(123L, savedPlayer.getId());
        assertEquals("SGS", savedPlayer.getName());
        assertEquals("TR", savedPlayer.getCountry());
        assertEquals(100, savedPlayer.getRating());
        assertEquals(456L, savedPlayer.getClubId());

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(123L, "Player");

        Player playerFromDB = playerRepository.findById(idMapping.getServiceId()).orElseThrow();
        assertEquals("SGS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(100, playerFromDB.getRating());
        assertEquals(club.getId(), playerFromDB.getClubId());
    }

    @Test
    public void testUpdateRating() {
        Club club = clubRepository.save(new Club("FB", "TR", "AK"));
        Player player = playerRepository.save(new Player("SGS", "TR", 100, club));

        idMappingRepository.save(new IdMapping(club.getId(), 456L, "Club"));
        idMappingRepository.save(new IdMapping(player.getId(), 789L, "Player"));

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
        Player playerFromDB = playerRepository.findById(player.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(200, playerFromDB.getRating());
        assertEquals(club.getId(), playerFromDB.getClubId());
    }

    @Test
    public void testTransferPlayer() {
        Club club1 = clubRepository.save(new Club("FB", "TR", "AK"));
        Club club2 = clubRepository.save(new Club("GS", "TR", "AY"));
        Player player = playerRepository.save(new Player("SGS", "TR", 100, club1));

        idMappingRepository.save(new IdMapping(club1.getId(), 456L, "Club"));
        idMappingRepository.save(new IdMapping(club2.getId(), 789L, "Club"));
        idMappingRepository.save(new IdMapping(player.getId(), 123L, "Player"));

        WireMock.stubFor(WireMock.put("/players/123/transfer").withRequestBody(WireMock.equalTo("789"))
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 123,
                                    "name": "SGS",
                                    "country": "TR",
                                    "rating": 100,
                                    "clubId": 789
                                }
                                """)));

        restTemplate.put("/players/123/transfer", 789);
        Player playerFromDB = playerRepository.findById(player.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(100, playerFromDB.getRating());
        assertEquals(club2.getId(), playerFromDB.getClubId());
    }

}
