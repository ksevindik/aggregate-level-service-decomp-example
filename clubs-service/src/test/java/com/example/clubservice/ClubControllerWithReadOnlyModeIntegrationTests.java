package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
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

public class ClubControllerWithReadOnlyModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
    }

    @Test
    public void testGetAllClubs() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs/country/ES",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        verifyGetResponse(response, testFixture.club2FromMonolith);
    }

    @Test
    public void testGetClubById() {
        ResponseEntity<Club> response = restTemplate.getForEntity("/clubs/" + testFixture.club1FromMonolith.getId(), Club.class);
        verifyGetResponse(response, testFixture.club1FromMonolith);
    }

    @Test
    public void testCreateClub() {
        Club club = new Club("RM", "ES", "XX");

        registerMonolithResponse("/clubs", "POST", """
                {
                    "name": "RM",
                    "country": "ES",
                    "president": "XX"
                }
                """, 201, """
                {
                    "id": 654,
                    "name": "RM",
                    "country": "ES",
                    "president": "XX"
                }
                """);
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        verifyClub(club, 654L, savedClub);
    }

    @Test
    public void testUpdatePresident() {
        registerMonolithResponse("/clubs/321/president", "PUT", "AY", 200, """
                {
                    "id": 321,
                    "name": "FB",
                    "country": "TR",
                    "president": "AY"
                }
                """);
        Club updatedClub = restTemplate.exchange("/clubs/321/president",
                HttpMethod.PUT, new HttpEntity<String>("AY"), Club.class).getBody();
        verifyClub(new Club("FB", "TR", "AY"), 321L, updatedClub);
    }
}
