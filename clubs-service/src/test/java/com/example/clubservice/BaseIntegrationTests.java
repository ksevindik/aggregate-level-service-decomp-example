package com.example.clubservice;

import com.example.clubservice.migration.EntityChangeEvent;
import com.example.clubservice.migration.EntityPersistedEvent;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.ClubRepository;
import com.example.clubservice.repository.IdMappingRepository;
import com.example.clubservice.repository.PlayerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, kraft = false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = "service.migration.monolith-base-url=http://localhost:${wiremock.server.port}")
@Import(BaseIntegrationTests.BaseTestConfig.class)
public abstract class BaseIntegrationTests {
    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private Long port;

    @Autowired
    protected OperationModeManager operationModeManager;

    @Autowired
    protected IdMappingRepository idMappingRepository;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    protected RestTemplate restTemplate;

    @Autowired
    protected ClubRepository clubRepository;

    @Autowired
    protected PlayerRepository playerRepository;

    @Autowired
    protected ObjectMapper objectMapper;

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

    protected void createIdMappings(IdMapping... idMappings) {
        for(IdMapping idMapping : idMappings) {
            idMappingRepository.save(idMapping);
        }
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

    protected void verifyClub(Club expected, Long expectedId, Club actual) {
        assertEquals(expectedId, actual.getId());
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

    protected Player findPlayerById(Long id) {
        return playerRepository.findById(id).orElseThrow();
    }

    @BeforeEach
    public void _setUp() {
        operationModeManager.setOperationMode(getOperationMode());
        restTemplate = restTemplateBuilder.rootUri("http://localhost:" + port).build();
        latchForChangeEventPublishes = new CountDownLatch(1);
        latchForEntityPersistedEvents = new CountDownLatch(1);
    }

    @AfterEach
    public void _tearDown() {
        idMappingRepository.deleteAll();
        playerRepository.deleteAll();
        clubRepository.deleteAll();
        WireMock.resetToDefault();
    }

    @BeforeAll
    static void _beforeAll() {
        // Configure WireMock to have a longer shutdown timeout
        //WireMockSpring.options().jettyStopTimeout(100000L).timeout(100000);
    }

    @AfterAll
    static void _afterAll() {
        eventMap.clear();
    }

    public void openDBConsole() {
        try {
            Server.startWebServer(DataSourceUtils.getConnection(dataSource));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract OperationMode getOperationMode();
}
