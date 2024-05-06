package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/*
the job of monolith read-write api adapter is to provide a convenient way to interact with the monolith side.
 */
@Component
public class MonolithReadWriteApiAdapter {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private MigrationProperties migrationProperties;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = restTemplateBuilder.rootUri(migrationProperties.getMonolithBaseUrl()).build();
    }

    //club related operations

    public Club createClub(Club club) {
        return restTemplate.postForObject("/clubs", club, Club.class);
    }

    public Club updatePresident(Long clubId, String president) {
        return restTemplate.exchange("/clubs/{clubId}/president",
                HttpMethod.PUT, new HttpEntity(president), Club.class, clubId).getBody();
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        return restTemplate.exchange(
                "/clubs/search?name={name}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Club>>() {}, namePattern).getBody();
    }

    public List<Club> getClubsByCountry(String country) {
        return restTemplate.exchange(
                "/clubs/country/{country}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Club>>() {}, country).getBody();
    }

    public List<Club> getAllClubs() {
        return restTemplate.exchange(
                "/clubs",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Club>>() {}).getBody();
    }

    public Club getClubById(Long clubId) {
        return restTemplate.getForObject("/clubs/{clubId}", Club.class, clubId);
    }

    //player related operations

    public Player createPlayer(Player player) {
        return restTemplate.postForObject("/players", player, Player.class);
    }

    public Player updatePlayerRating(Long playerId, Integer rating) {
        return restTemplate.exchange("/players/{playerId}/rating",
                HttpMethod.PUT, new HttpEntity(rating), Player.class, playerId).getBody();
    }

    public Player transferPlayer(Long playerId, Long clubId) {
        return restTemplate.exchange("/players/{playerId}/transfer",
                HttpMethod.PUT, new HttpEntity(clubId), Player.class, playerId).getBody();
    }

    public Player getPlayerById(Long playerId) {
        return restTemplate.getForObject("/players/{playerId}", Player.class, playerId);
    }

    public List<Player> getAllPlayers() {
        return restTemplate.exchange(
                "/players",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Player>>() {}).getBody();
    }

    public List<Player> getPlayersByClubName(String clubName) {
        return restTemplate.exchange(
                "/players/clubName?clubName={clubName}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Player>>() {}, clubName).getBody();
    }

    public List<Player> getPlayersByCountry(String country) {
        return restTemplate.exchange(
                "/players/country/{country}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Player>>() {}, country).getBody();
    }

    public List<Player> getPlayersByNamePattern(String name) {
        return restTemplate.exchange(
                "/players/search?name={name}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Player>>() {}, name).getBody();
    }
}
