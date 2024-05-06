package com.example.clubservice.base;

import com.example.clubservice.TestFixture;
import com.example.clubservice.migration.EntityChangeEvent;
import com.example.clubservice.migration.EntityPersistedEvent;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.ClubRepository;
import com.example.clubservice.repository.IdMappingRepository;
import com.example.clubservice.repository.PlayerRepository;
import com.example.clubservice.utils.EventPublishingWireMockConfigurationCustomizer;
import com.example.clubservice.utils.MapProxy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(BaseOperationModeIntegrationTests.BaseTestConfig.class)
public abstract class BaseOperationModeIntegrationTests extends BaseIntegrationTests {
    protected abstract OperationMode getOperationMode();

    @Autowired
    protected OperationModeManager operationModeManager;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ClubRepository clubRepository;

    @Autowired
    protected PlayerRepository playerRepository;

    @Autowired
    protected IdMappingRepository idMappingRepository;

    @TestConfiguration
    static class BaseTestConfig {
        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper)  {
            return new EventPublishingWireMockConfigurationCustomizer(kafkaTemplate, objectMapper);
        }
    }

    private static CountDownLatch latchForChangeEventPublishes = new CountDownLatch(1);
    private static CountDownLatch latchForEntityPersistedEvents = new CountDownLatch(1);
    private static Map<String, EntityChangeEvent> eventMap = MapProxy.createProxy(new HashMap<>());

    protected TestFixture testFixture;

    @BeforeEach
    public void __setUp() {
        testFixture = new TestFixture(clubRepository, playerRepository, idMappingRepository);
        operationModeManager.setOperationMode(getOperationMode());
        latchForChangeEventPublishes = new CountDownLatch(1);
        latchForEntityPersistedEvents = new CountDownLatch(1);
    }

    @AfterEach
    public void __tearDown() {
        idMappingRepository.deleteAll();
        playerRepository.deleteAll();
        clubRepository.deleteAll();
    }

    @AfterAll
    static void __afterAll() {
        eventMap.clear();
    }

    @KafkaListener(topics = "entity-change-topic", groupId = "club-service-tests")
    public void handle(String message) throws JsonProcessingException {
        EntityChangeEvent entityChangeEvent = objectMapper.readValue(message, EntityChangeEvent.class);
        eventMap.put(entityChangeEvent.getAction(), entityChangeEvent);
        latchForChangeEventPublishes.countDown();
    }

    @TransactionalEventListener
    public void handle(EntityPersistedEvent event) {
        latchForEntityPersistedEvents.countDown();
    }

    protected void waitForEntityChangeEvenToBetPublished() {
        try {
            latchForChangeEventPublishes.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void waitForEntityPersistedEvent() {
        try {
            latchForEntityPersistedEvents.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void verifyEntityChangeEvent(Object entity, String operation) {
        try {
            EntityChangeEvent entityChangeEvent = eventMap.get(operation);
            assertEquals("service", entityChangeEvent.getOrigin());
            assertEquals(entity.getClass().getSimpleName(), entityChangeEvent.getType());
            assertEquals(operation, entityChangeEvent.getAction());
            assertEquals(entity, objectMapper.readValue(entityChangeEvent.getEntity(), entity.getClass()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
