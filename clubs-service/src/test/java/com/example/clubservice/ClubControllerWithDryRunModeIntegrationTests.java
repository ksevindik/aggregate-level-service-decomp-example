package com.example.clubservice;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.model.Club;
import com.example.clubservice.repository.ClubRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = "service.migration.monolith-base-url=http://localhost:${wiremock.server.port}")
public class ClubControllerWithDryRunModeIntegrationTests extends BaseIntegrationTests {

    @Autowired
    private OperationModeManager operationModeManager;

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
                                    "id": 111,
                                    "name": "FB",
                                    "country": "TR",
                                    "president": "AK"
                                }
                                """)));
        Club savedClub = restTemplate.postForObject("/clubs", club, Club.class);
        assertEquals(111L, savedClub.getId());
        assertEquals("FB", savedClub.getName());
        assertEquals("TR", savedClub.getCountry());
        assertEquals("AK", savedClub.getPresident());

        Club clubFromDB = clubRepository.findAll().get(0);
        assertEquals("FB", clubFromDB.getName());
        assertEquals("TR", clubFromDB.getCountry());
        assertEquals("AK", clubFromDB.getPresident());
    }

    @Test
    public void testUpdatePresident() {
        Club club = new Club();
        club.setId(456L);
        club.setName("FB");
        club.setCountry("TR");
        club.setPresident("AK");
        clubRepository.save(club);

        WireMock.stubFor(WireMock.put("/clubs/123/president")
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
        Club clubFromDB = clubRepository.findAll().get(0);
        assertEquals("FB", clubFromDB.getName());
        assertEquals("TR", clubFromDB.getCountry());
        assertEquals("AY", clubFromDB.getPresident());
    }
}
