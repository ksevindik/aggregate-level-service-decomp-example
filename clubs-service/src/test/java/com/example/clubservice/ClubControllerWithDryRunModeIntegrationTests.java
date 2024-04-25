package com.example.clubservice;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.repository.ClubRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClubControllerWithDryRunModeIntegrationTests extends BaseIntegrationTests {

    @Autowired
    private ClubRepository clubRepository;

    @BeforeEach
    public void setUp() {
        operationModeManager.setOperationMode(OperationMode.DRY_RUN);
        String responseBodyForGetAllClubs = """
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
                """;
        String responseBodyForGetClubsByCountry = """
                [
                    {
                        "id": 123
                    }
                ]
                """;
        String responseBodyForGetClubById = """
                {
                    "id": 123
                }
                """;
        WireMock.stubFor(WireMock.get("/clubs")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetAllClubs)));

        WireMock.stubFor(WireMock.get("/clubs/country/TR")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetClubsByCountry)));

        WireMock.stubFor(WireMock.get("/clubs/123")
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody(responseBodyForGetClubById)));
    }

    @AfterEach
    public void tearDown() {
        clubRepository.deleteAll();
        idMappingRepository.deleteAll();
    }

    @Test
    public void testGetAllClubs() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(3, response.getBody().size());
        MatcherAssert.assertThat(response.getBody().stream().map(c->c.getId()).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(123L, 456L, 789L));
    }

    @Test
    public void testGetClubsByCountry() {
        ResponseEntity<List<Club>> response = restTemplate.exchange("/clubs/country/TR",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(123L, response.getBody().get(0).getId());
    }

    @Test
    public void testGetClubById() {
        Club club = restTemplate.getForObject("/clubs/123", Club.class);
        assertEquals(123L, club.getId());
    }

    @Test
    public void testCreateClub() {
        Club club = new Club();
        club.setName("FB");
        club.setCountry("TR");
        club.setPresident("AK");
        WireMock.stubFor(WireMock.post("/clubs")
                .willReturn(WireMock.aResponse().withStatus(201)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 123,
                                    "name": "FB",
                                    "country": "TR",
                                    "president": "AK"
                                }
                                """)));
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        assertEquals(123L, savedClub.getId());
        assertEquals("FB", savedClub.getName());
        assertEquals("TR", savedClub.getCountry());
        assertEquals("AK", savedClub.getPresident());

        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(123L, "Club");

        Club clubFromDB = clubRepository.findById(idMapping.getServiceId()).orElseThrow();
        assertEquals("FB", clubFromDB.getName());
        assertEquals("TR", clubFromDB.getCountry());
        assertEquals("AK", clubFromDB.getPresident());
    }

    @Test
    public void testUpdatePresident() {
        Club club = new Club();
        club.setName("FB");
        club.setCountry("TR");
        club.setPresident("AK");
        club = clubRepository.save(club);

        idMappingRepository.save(new IdMapping(club.getId(), 123L, "Club"));

        WireMock.stubFor(WireMock.put("/clubs/123/president")
                        .withRequestBody(WireMock.equalTo("AY"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                    "id": 123,
                                    "name": "FB",
                                    "country": "TR",
                                    "president": "AY"
                                }
                                """)));
        restTemplate.put("/clubs/123/president", "AY");

        Club clubFromDB = clubRepository.findById(club.getId()).orElseThrow();
        assertEquals("FB", clubFromDB.getName());
        assertEquals("TR", clubFromDB.getCountry());
        assertEquals("AY", clubFromDB.getPresident());
    }
}
