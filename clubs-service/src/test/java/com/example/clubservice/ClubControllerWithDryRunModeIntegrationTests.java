package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClubControllerWithDryRunModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.DRY_RUN;
    }

    @Test
    public void testGetAllClubs() {
        registerMonolithResponse("/clubs", "GET", null, 200, """
                [
                    {
                        "id": 456,
                        "name": "GS",
                        "country": "TR",
                        "president": "FT"
                    },
                    {
                        "id": 123,
                        "name": "BJK",
                        "country": "ES",
                        "president": "FU"
                    },
                    {
                        "id": 321,
                        "name": "FB",
                        "country": "TR",
                        "president": "AK"
                    }
                ]
                """);
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        registerMonolithResponse("/clubs/country/TR", "GET", null, 200, """
                [
                    {
                        "id": 456,
                        "name": "GS",
                        "country": "TR",
                        "president": "FT"
                    },
                    {
                        "id": 321,
                        "name": "FB",
                        "country": "TR",
                        "president": "AK"
                    }
                ]
                """);
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs/country/TR",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubById() {
        registerMonolithResponse("/clubs/123", "GET", null, 200, """
                    {
                        "id": 123,
                        "name": "BJK",
                        "country": "ES",
                        "president": "FU"
                    }
                """);
        ResponseEntity<Club> response = restTemplate.getForEntity("/clubs/123", Club.class);
        verifyGetResponse(response, testFixture.club2FromMonolith);
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

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(654L, "Club");

        Club clubFromDB = clubRepository.findById(idMapping.getServiceId()).orElseThrow();
        verifyClub(new Club("RM", "ES", "XX"), idMapping.getServiceId(), clubFromDB);
    }

    @Test
    public void testUpdatePresident() {
        registerMonolithResponse("/clubs/321/president", "PUT", "AY", 200, """
                {
                    "id": 321,
                    "name": "FB",
                    "country": "TR",
                    "president": "AK"
                }
                """);
        restTemplate.put("/clubs/321/president", "AY");

        Club clubFromDB = clubRepository.findById(testFixture.club3.getId()).orElseThrow();
        verifyClub(new Club("FB", "TR", "AY"), testFixture.club3.getId(), clubFromDB);
    }
}
