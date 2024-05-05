package com.example.clubservice;

import com.example.clubservice.base.BaseOperationModeIntegrationTests;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
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

public class ClubControllerWithReadWriteModeIntegrationTests extends BaseOperationModeIntegrationTests {

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_WRITE;
    }

    @Test
    public void testGetAllClubs() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        verifyGetResponse(response, testFixture.club1, testFixture.club2, testFixture.club3);
    }

    @Test
    public void testGetClubsByCountry() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs/country/ES",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        verifyGetResponse(response, testFixture.club2);
    }

    @Test
    public void testGetClubById() {
        ResponseEntity<Club> response = restTemplate.getForEntity("/clubs/" + testFixture.club1.getId(), Club.class);
        verifyGetResponse(response, testFixture.club1);
    }

    @Test
    public void testCreateClub() throws InterruptedException {
        Club club = new Club("RM", "ES", "XX");

        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        Club clubFromDB = clubRepository.findById(savedClub.getId()).orElseThrow();

        verifyClub(club, savedClub.getId(), savedClub);
        verifyClub(clubFromDB, clubFromDB.getId(), savedClub);

        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(clubFromDB, "CREATE");
    }



    @Test
    public void testUpdatePresident() {
        registerMonolithResponse("/clubs/456","GET",null,200,"""
                {
                    "id": 456,
                    "name": "GS",
                    "country": "TRY",
                    "president": "FT",
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);


        restTemplate.put("/clubs/"+ testFixture.club1.getId() + "/president", "AY");

        Club clubFromDB = clubRepository.findById(testFixture.club1.getId()).orElseThrow();
        verifyClub(new Club("GS", "TRY", "AY"), testFixture.club1.getId(), clubFromDB);
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(clubFromDB, "UPDATE");
    }
}
