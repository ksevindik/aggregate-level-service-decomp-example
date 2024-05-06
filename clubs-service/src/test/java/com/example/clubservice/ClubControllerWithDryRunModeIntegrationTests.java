package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ClubControllerWithDryRunModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.DRY_RUN;
    }

    @Test
    public void testGetAllClubs() {
        /*
        clubs should be retrieved from the monolith side
         */
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
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club2FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubsByCountry() {
        /*
        clubs should be retrieved from the monolith side
         */
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
        ResponseEntity<List<Club>> response = performGetClubsRequest("/clubs/country/TR");
        verifyGetResponse(response, testFixture.club1FromMonolith, testFixture.club3FromMonolith);
    }

    @Test
    public void testGetClubById() {
        /*
        clubs should be retrieved from the monolith side
         */
        registerMonolithResponse("/clubs/123", "GET", null, 200, """
                    {
                        "id": 123,
                        "name": "BJK",
                        "country": "ES",
                        "president": "FU"
                    }
                """);
        ResponseEntity<Club> response = performGetClubRequest("/clubs/123");
        verifyGetResponse(response, testFixture.club2FromMonolith);
    }

    @Test
    public void testCreateClub() {
        /*
        club should be created on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */
        registerMonolithResponse("/clubs", "POST", """
                {
                    "name": "DR",
                    "country": "ES",
                    "president": "XX"
                }
                """, 201, """
                {
                    "id": 444,
                    "name": "DR",
                    "country": "ES",
                    "president": "XX"
                }
                """);
        Club club = new Club("DR", "ES", "XX");
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        verifyClub(new Club(444L,"DR", "ES", "XX"), savedClub);

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(444L, "Club");

        Club clubFromDB = clubRepository.findById(idMapping.getServiceId()).orElseThrow();
        verifyClub(new Club(idMapping.getServiceId(),"DR", "ES", "XX"), clubFromDB);
    }

    @Test
    public void testUpdatePresident() {
        /*
        club should be updated on the both sides at the same time
        entity change events published from the monolith side should be ignored
        no entity change event should be published from the service side at this step
         */
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
        verifyClub(new Club(testFixture.club3.getId(), "FB", "TR", "AY"), clubFromDB);
    }
}
