package com.example.clubservice;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.model.Club;
import com.github.tomakehurst.wiremock.client.WireMock;
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
    }

    @Test
    public void testUpdatePresident() {
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
        Club updatedClub = restTemplate.exchange("/clubs/123/president",
                HttpMethod.PUT, new HttpEntity<String>("AY"), Club.class).getBody();
        assertEquals(123L, updatedClub.getId());
        assertEquals("FB", updatedClub.getName());
        assertEquals("TR", updatedClub.getCountry());
        assertEquals("AY", updatedClub.getPresident());
    }
}
