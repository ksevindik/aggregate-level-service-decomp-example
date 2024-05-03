package com.example.clubservice;

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

public class ClubControllerWithReadOnlyModeIntegrationTests extends BaseIntegrationTests {

    private Club club1,club2, club3;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_ONLY;
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
    public void testCreateClub() {
        Club club = new Club("FB", "TR", "AK");

        registerMonolithResponse("/clubs", "POST", """
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
    }

    @Test
    public void testUpdatePresident() {
        registerMonolithResponse("/clubs/123/president", "PUT", "AY", 200, """
                {
                    "id": 123,
                    "name": "FB",
                    "country": "TR",
                    "president": "AY"
                }
                """);
        Club updatedClub = restTemplate.exchange("/clubs/123/president",
                HttpMethod.PUT, new HttpEntity<String>("AY"), Club.class).getBody();
        verifyClub(new Club("FB", "TR", "AY"), 123L, updatedClub);
    }
}
