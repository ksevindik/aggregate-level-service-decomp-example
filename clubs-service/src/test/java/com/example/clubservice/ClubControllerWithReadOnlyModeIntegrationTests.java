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
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs/country/ES");
        verifyGetResponse(response, testFixture.club2FromMonolith);
    }

    @Test
    public void testGetClubById() {
        /*
        there should be no interaction with the monolith side
        all the result should be retrieved from the service side, however entity ids should be of monolith side
         */
        ResponseEntity<Club> response = performGetClubRequest("/clubs/" + testFixture.club1FromMonolith.getId());
        verifyGetResponse(response, testFixture.club1FromMonolith);
    }

    @Test
    public void testCreateClub() {
        /*
        new club should be created on the monolith side
        entity change event published from the monolith side should be consumed and club should be persisted on the service side
        entity ids should be from the monolith side
         */
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
        verifyClub(new Club(654L,"RM", "ES", "XX"), savedClub);
    }

    @Test
    public void testUpdatePresident() {
        /*
        club should be updated on the monolith side
        entity change event published from the monolith side should be consumed and club should be updated on the service side
        entity ids should be from the monolith side
         */
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
        verifyClub(new Club(321L,"FB", "TR", "AY"), updatedClub);
    }
}
