package com.example.clubservice;

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

    private Club club1,club2, club3;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_WRITE;
    }

    @BeforeEach
    public void setUp() {
        club1 = clubRepository.save(new Club("GS", "TR", "FT"));
        club2 = clubRepository.save(new Club("BJK", "TR", "FU"));
        club3 = clubRepository.save(new Club("RM", "ES", "FP"));
    }

    @Test
    public void testGetAllClubs() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(3, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(c->c.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(club1.getId(), club2.getId(), club3.getId()));
    }

    @Test
    public void testGetClubsByCountry() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs/country/ES",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(club3.getId(), response.getBody().get(0).getId());
    }

    @Test
    public void testGetClubById() {
        Club club = restTemplate.getForObject("/clubs/" + club1.getId(), Club.class);
        assertEquals(club1.getId(), club.getId());
    }

    @Test
    public void testCreateClub() throws InterruptedException {
        Club club = new Club("FB", "TR", "AK");

        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        Club clubFromDB = clubRepository.findById(savedClub.getId()).orElseThrow();

        verifyClub(club, savedClub.getId(), savedClub);
        verifyClub(clubFromDB, clubFromDB.getId(), savedClub);

        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(clubFromDB, "CREATE");
    }



    @Test
    public void testUpdatePresident() {
        idMappingRepository.save(new IdMapping(club1.getId(), 123L, "Club"));

        registerMonolithResponse("/clubs/123","GET",null,200,"""
                {
                    "id": 123,
                    "name": "GS",
                    "country": "TRY",
                    "president": "FT",
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """);


        restTemplate.put("/clubs/"+ club1.getId() + "/president", "AY");

        Club clubFromDB = clubRepository.findById(club1.getId()).orElseThrow();
        verifyClub(new Club("GS", "TRY", "AY"), club1.getId(), clubFromDB);
        waitForEntityChangeEvenToBetPublished();
        verifyEntityChangeEvent(clubFromDB, "UPDATE");
    }
}
