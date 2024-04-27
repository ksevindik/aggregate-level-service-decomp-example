package com.example.clubservice;

import com.example.clubservice.migration.EntityChangeEvent;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.model.Club;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import wiremock.org.eclipse.jetty.http.HttpMethod;

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

    private static CountDownLatch latch = new CountDownLatch(1);
    private static Map<String, EntityChangeEvent> eventMap = MapProxy.createProxy(new HashMap<>());

    @KafkaListener(topics = "entity-change-topic", groupId = "club-service-tests")
    public void handle(String message) throws JsonProcessingException {
        EntityChangeEvent entityChangeEvent = objectMapper.readValue(message, EntityChangeEvent.class);
        eventMap.put(entityChangeEvent.getAction(), entityChangeEvent);
        latch.countDown();
    }

    protected void waitForKafka() {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void verifyEntityChangeEventForClub(Club entity, String operation) {
        try {
            EntityChangeEvent entityChangeEvent = eventMap.get(operation);
            assertEquals("service", entityChangeEvent.getOrigin());
            assertEquals("Club", entityChangeEvent.getType());
            assertEquals(operation, entityChangeEvent.getAction());
            assertEquals(entity, objectMapper.readValue(entityChangeEvent.getEntity(), Club.class));
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

    protected void trainWireMock(String url, String method, String requestBody, int status, String responseBody) {
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

    @BeforeEach
    public void _setUp() {
        operationModeManager.setOperationMode(getOperationMode());
        restTemplate = restTemplateBuilder.rootUri("http://localhost:" + port).build();
        latch = new CountDownLatch(1);
    }

    @AfterEach
    public void _tearDown() {
        idMappingRepository.deleteAll();
        playerRepository.deleteAll();
        clubRepository.deleteAll();
    }

    @BeforeAll
    static void _beforeAll() {
        // Configure WireMock to have a longer shutdown timeout
        WireMockSpring.options().jettyStopTimeout(100000L).timeout(100000);
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
