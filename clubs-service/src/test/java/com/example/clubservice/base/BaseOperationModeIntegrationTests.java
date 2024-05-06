package com.example.clubservice.base;

import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.ClubRepository;
import com.example.clubservice.repository.IdMappingRepository;
import com.example.clubservice.repository.PlayerRepository;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(value= {EventPublishingWireMockConfigurationCustomizer.class, TestEntityChangeEventHandler.class, TestEntityPersistEventHandler.class})
public abstract class BaseOperationModeIntegrationTests extends BaseIntegrationTests {
    protected abstract OperationMode getOperationMode();

    @Autowired
    protected OperationModeManager operationModeManager;

    @Autowired
    protected ClubRepository clubRepository;

    @Autowired
    protected PlayerRepository playerRepository;

    @Autowired
    protected IdMappingRepository idMappingRepository;

    protected TestFixture testFixture;

    @Autowired
    private TestEntityChangeEventHandler testEntityChangeEventHandler;

    @Autowired
    private TestEntityPersistEventHandler testEntityPersistEventHandler;

    @BeforeEach
    public void __setUp() {
        testFixture = new TestFixture(clubRepository, playerRepository, idMappingRepository);
        operationModeManager.setOperationMode(getOperationMode());
    }

    @AfterEach
    public void __tearDown() {
        idMappingRepository.deleteAll();
        playerRepository.deleteAll();
        clubRepository.deleteAll();
        testEntityChangeEventHandler.reset();
        testEntityPersistEventHandler.reset();
    }

    protected void waitForEntityChangeEvenToBetPublished() {
        testEntityChangeEventHandler.waitForEntityChangeEvenToBetPublished();
    }

    protected void waitForEntityPersistedEvent() {
        testEntityPersistEventHandler.waitForEntityPersistedEvent();
    }

    protected void verifyEntityChangeEvent(Object entity, String operation) {
        testEntityChangeEventHandler.verifyEntityChangeEvent(entity, operation);
    }

    protected void verifyGetResponse(ResponseEntity<?> response, Object...entities) {
        assertEquals(200, response.getStatusCodeValue());
        Object body = response.getBody();
        if(body instanceof List) {
            List<?> bodyList = (List<?>) body;
            assertEquals(entities.length, bodyList.size());
            MatcherAssert.assertThat(bodyList.stream().collect(Collectors.toList()), Matchers.containsInAnyOrder(entities));
        } else {
            assertEquals(entities[0], body);
        }
    }

    protected ResponseEntity<List<Player>> performGetPlayersRequest(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Player>>() {});
    }

    protected ResponseEntity<Player> performGetPlayerRequest(String url) {
        return restTemplate.getForEntity(url, Player.class);
    }

    protected ResponseEntity<List<Club>> performGetClubsRequest(String url) {
        return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Club>>() {});
    }

    protected ResponseEntity<Club> performGetClubRequest(String url) {
        return restTemplate.getForEntity(url, Club.class);
    }

    protected Player findPlayerById(Long id) {
        return playerRepository.findById(id).orElseThrow();
    }

    protected void verifyClub(Club expected, Club actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getCountry(), actual.getCountry());
        assertEquals(expected.getPresident(), actual.getPresident());
    }

    protected void verifyPlayer(Player expected, Player actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getCountry(), actual.getCountry());
        assertEquals(expected.getRating(), actual.getRating());
        assertEquals(expected.getClubId(), actual.getClubId());
    }

    protected void registerMonolithResponse(String url, String method, String requestBody, int status, String responseBody) {
        MappingBuilder mappingBuilder = WireMock.request(method, WireMock.urlEqualTo(url));
        if(requestBody != null) {
            if(method.equals("PUT")) {
                mappingBuilder.withRequestBody(WireMock.equalTo(requestBody));
            } else {
                mappingBuilder.withRequestBody(WireMock.equalToJson(requestBody, true, true));
            }
        }

        WireMock.stubFor(mappingBuilder.willReturn(WireMock.aResponse().withStatus(status)
                .withHeader("Content-Type","application/json")
                .withBody(responseBody)));
    }
}
