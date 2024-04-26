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

public class ClubControllerWithReadWriteModeIntegrationTests extends BaseIntegrationTests{

    private Club club1,club2, club3;

    @Override
    protected OperationMode getOperationMode() {
        return OperationMode.READ_WRITE;
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
        club3 = new Club();
        club3.setName("RM");
        club3.setCountry("ES");
        club3.setPresident("FP");
        club3 = clubRepository.save(club3);
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
        Club club = new Club();
        club.setName("FB");
        club.setCountry("TR");
        club.setPresident("AK");

        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        Club clubFromDB = clubRepository.findById(savedClub.getId()).orElseThrow();

        assertEquals(clubFromDB.getId(), savedClub.getId());

        assertEquals("FB", savedClub.getName());
        assertEquals("TR", savedClub.getCountry());
        assertEquals("AK", savedClub.getPresident());


        assertEquals("FB", clubFromDB.getName());
        assertEquals("TR", clubFromDB.getCountry());
        assertEquals("AK", clubFromDB.getPresident());
    }

    @Test
    public void testUpdatePresident() {
        idMappingRepository.save(new IdMapping(club1.getId(), 123L, "Club"));

        String responseBodyForGetClubById = """
                {
                    "id": 123,
                    "name": "GS",
                    "country": "TRY",
                    "president": "FT",
                    "created": "2021-07-01T00:00:00",
                    "modified": "2021-07-01T00:00:00"
                }
                """;

        WireMock.stubFor(WireMock.get("/clubs/123")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetClubById)));


        restTemplate.put("/clubs/"+ club1.getId() + "/president", "AY");

        Club clubFromDB = clubRepository.findById(club1.getId()).orElseThrow();
        assertEquals("GS", clubFromDB.getName());
        assertEquals("TRY", clubFromDB.getCountry());
        assertEquals("AY", clubFromDB.getPresident());
    }
}
