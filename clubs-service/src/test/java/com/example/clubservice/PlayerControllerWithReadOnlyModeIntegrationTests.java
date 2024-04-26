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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PlayerControllerWithReadOnlyModeIntegrationTests extends BaseIntegrationTests {

    private Club club1, club2;
    private Player player1, player2, player3, player4;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
    }

    @BeforeEach
    public void setUp() {
        club1 = new Club();
        club1.setName("GS");
        club1.setCountry("TR");
        club1.setPresident("FT");
        club1 = clubRepository.save(club1);

        club2 = new Club();
        club2.setName("BJK");
        club2.setCountry("TR");
        club2.setPresident("FU");
        club2 = clubRepository.save(club2);

        player1 = new Player();
        player1.setName("SGS");
        player1.setRating(100);
        player1.setCountry("TR");
        player1.setClub(club1);
        player1 = playerRepository.save(player1);

        player2 = new Player();
        player2.setName("SYS");
        player2.setRating(90);
        player2.setCountry("TR");
        player2.setClub(club1);
        player2 = playerRepository.save(player2);

        player3 = new Player();
        player3.setName("HS");
        player3.setRating(80);
        player3.setCountry("US");
        player3.setClub(club2);
        player3 = playerRepository.save(player3);

        player4 = new Player();
        player4.setName("KS");
        player4.setRating(70);
        player4.setCountry("DE");
        player4.setClub(null);
        player4 = playerRepository.save(player4);
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
        Player player = new Player();
        player.setName("BS");
        player.setCountry("TR");
        player.setRating(100);
        player.setClubId(club2.getId());

        idMappingRepository.save(new IdMapping(club2.getId(), 456L, "Club"));

        WireMock.stubFor(WireMock.post("/players")
                .willReturn(WireMock.aResponse().withStatus(201)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 123,
                                    "name": "BS",
                                    "country": "TR",
                                    "rating": 100,
                                    "clubId": 456
                                }
                                """)));

        Player savedPlayer = restTemplate.postForObject("/players", player, Player.class);
        assertEquals(123L, savedPlayer.getId());
        assertEquals("BS", savedPlayer.getName());
        assertEquals("TR", savedPlayer.getCountry());
        assertEquals(100, savedPlayer.getRating());
        assertEquals(456L, savedPlayer.getClubId());
    }

    @Test
    public void testUpdateRating() {
        idMappingRepository.save(new IdMapping(club1.getId(), 456L, "Club"));
        idMappingRepository.save(new IdMapping(player1.getId(), 789L, "Player"));

        WireMock.stubFor(WireMock.put("/players/" + player1.getId() + "/rating").withRequestBody(WireMock.equalTo("200"))
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

        Player updatedPlayer = restTemplate.exchange(
                "/players/" + player1.getId() +"/rating",
                HttpMethod.PUT, new HttpEntity<Integer>(200),Player.class).getBody();


        assertEquals("SGS", updatedPlayer.getName());
        assertEquals("TR", updatedPlayer.getCountry());
        assertEquals(200, updatedPlayer.getRating());
        assertEquals(club1.getId(), updatedPlayer.getClubId());
    }

    @Test
    public void testTransferPlayer() {
        idMappingRepository.save(new IdMapping(club1.getId(), 456L, "Club"));
        idMappingRepository.save(new IdMapping(club2.getId(), 123L, "Club"));
        idMappingRepository.save(new IdMapping(player1.getId(), 789L, "Player"));

        WireMock.stubFor(WireMock.put("/players/" + player1.getId() + "/transfer")
                .withRequestBody(WireMock.equalTo(club2.getId().toString()))
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


        restTemplate.put("/players/" + player1.getId() + "/transfer", club2.getId());
        Player playerFromDB = playerRepository.findById(player1.getId()).orElseThrow();

        assertEquals("SGS", playerFromDB.getName());
        assertEquals("TR", playerFromDB.getCountry());
        assertEquals(100, playerFromDB.getRating());
        assertEquals(club2.getId(), playerFromDB.getClubId());
    }

}
