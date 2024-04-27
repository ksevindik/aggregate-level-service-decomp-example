package com.example.clubservice;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
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


public class ClubControllerWithDryRunModeIntegrationTests extends BaseIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.DRY_RUN;
    }

    @Test
    public void testGetAllClubs() {
        trainWireMock("/clubs", "GET", null, 200, """
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
                """);
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(3, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(c->c.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(123L, 456L, 789L));
    }

    @Test
    public void testGetClubsByCountry() {
        trainWireMock("/clubs/country/TR", "GET", null, 200, """
                [
                    {
                        "id": 123
                    }
                ]
                """);
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs/country/TR",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(123L, response.getBody().get(0).getId());
    }

    @Test
    public void testGetClubById() {
        trainWireMock("/clubs/123", "GET", null, 200, """
                {
                    "id": 123
                }
                """);
        Club club = restTemplate.getForObject("/clubs/123", Club.class);
        assertEquals(123L, club.getId());
    }

    @Test
    public void testCreateClub() {
        Club club = new Club("FB", "TR", "AK");
        trainWireMock("/clubs", "POST", """
                {
                    "name": "FB",
                    "country": "TR",
                    "president": "AK"
                }
                """, 201, """
                {
                    "id": 123,
                    "name": "FB",
                    "country": "TR",
                    "president": "AK"
                }
                """);
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        verifyClub(club, 123L, savedClub);

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(123L, "Club");

        Club clubFromDB = clubRepository.findById(idMapping.getServiceId()).orElseThrow();
        verifyClub(club, idMapping.getServiceId(), clubFromDB);
    }

    @Test
    public void testUpdatePresident() {
        Club club = new Club("FB", "TR", "AK");
        club = clubRepository.save(club);

        idMappingRepository.save(new IdMapping(club.getId(), 123L, "Club"));

        trainWireMock("/clubs/123/president", "PUT", "AY", 200, """
                {
                    "id": 123,
                    "name": "FB",
                    "country": "TR",
                    "president": "AK"
                }
                """);
        restTemplate.put("/clubs/123/president", "AY");

        Club clubFromDB = clubRepository.findById(club.getId()).orElseThrow();
        verifyClub(new Club("FB", "TR", "AY"), club.getId(), clubFromDB);
    }
}
