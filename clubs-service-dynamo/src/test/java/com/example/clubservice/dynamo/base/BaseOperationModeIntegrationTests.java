package com.example.clubservice.dynamo.base;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.example.clubservice.dynamo.migration.OperationMode;
import com.example.clubservice.dynamo.migration.OperationModeManager;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(value= {EventPublishingWireMockConfigurationCustomizer.class, TestMonolithEntityChangeEventHandler.class, TestEntityPersistEventHandler.class})
public abstract class BaseOperationModeIntegrationTests extends BaseIntegrationTests {
    protected abstract OperationMode getOperationMode();

    @Autowired
    protected OperationModeManager operationModeManager;

    protected TestFixture testFixture;

    @Autowired
    private TestMonolithEntityChangeEventHandler testMonolithEntityChangeEventHandler;

    @Autowired
    private TestEntityPersistEventHandler testEntityPersistEventHandler;

    @BeforeEach
    public void __setUp() {
        testFixture = new TestFixture(dynamoDBMapper);
        operationModeManager.setOperationMode(getOperationMode());
    }

    @AfterEach
    public void __tearDown() {
        testMonolithEntityChangeEventHandler.reset();
        testEntityPersistEventHandler.reset();
    }

    protected void waitForEntityChangeEvenToBetPublished() {
        testMonolithEntityChangeEventHandler.waitForEntityChangeEvenToBetPublished();
    }

    protected void waitForEntityPersistedEvent() {
        testEntityPersistEventHandler.waitForEntityPersistedEvent();
    }

    protected void verifyEntityChangeEvent(Object entity, String operation) {
        testMonolithEntityChangeEventHandler.verifyEntityChangeEvent(entity, operation);
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
        return dynamoDBMapper.load(ClubPlayerItem.class, "PLAYER#" + id, "PLAYER#" + id).toPlayer();
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

    protected Timestamp toTimestamp(String date) {
        try {
            return new Timestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(date).getTime());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected Optional<Club> findById(Long id) {
        ClubPlayerItem item = dynamoDBMapper.load(ClubPlayerItem.class, "CLUB#" + id, "CLUB#" + id);
        if(item == null) {
            return Optional.empty();
        }
        return Optional.of(item.toClub());
    }

    protected Optional<Club> findByMonolithId(Long id) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.withFilterExpression("#monolithId = :val1 and begins_with(#sk, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":val1", new AttributeValue().withN(id.toString()),
                        ":skPrefix", new AttributeValue().withS("CLUB#")))
                .withExpressionAttributeNames(Map.of("#sk", "SK", "#monolithId", "monolithId"));
        List<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        if(items.isEmpty()) {
            return Optional.empty();
        } else if(items.size()>1) {
            throw new IllegalStateException("Multiple clubs found with monolith id: " + id);
        }
        return Optional.of(items.get(0).toClub());
    }

    protected Optional<Player> findPlayerByMonolithId(Long id) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        scanExpression.withFilterExpression("#monolithId = :val1 and begins_with(#sk, :skPrefix)")
                .withExpressionAttributeValues(Map.of(
                        ":val1", new AttributeValue().withN(id.toString()),
                        ":skPrefix", new AttributeValue().withS("PLAYER#")))
                .withExpressionAttributeNames(Map.of("#sk", "SK", "#monolithId", "monolithId"));
        List<ClubPlayerItem> items = dynamoDBMapper.scan(ClubPlayerItem.class, scanExpression);
        if(items.isEmpty()) {
            return Optional.empty();
        } else if(items.size()>1) {
            throw new IllegalStateException("Multiple players found with monolith id: " + id);
        }
        return Optional.of(items.get(0).toPlayer());
    }
}
