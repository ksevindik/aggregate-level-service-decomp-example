package com.example.clubservice.dynamo.base;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.SSESpecification;
import com.amazonaws.services.dynamodbv2.model.SSEType;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.dynamodbv2.model.Tag;
import com.example.clubservice.dynamo.service.ClubService;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, kraft = false)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = "service.migration.monolith-base-url=http://localhost:${wiremock.server.port}")
public abstract class BaseIntegrationTests {

    @LocalServerPort
    private Long port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    protected RestTemplate restTemplate;

    @BeforeEach
    public void _setUp() {
        restTemplate = restTemplateBuilder.rootUri("http://localhost:" + port).build();

        CreateTableRequest createTableRequest =	new CreateTableRequest();
        createTableRequest
                .withTableName("ClubPlayerTable")
                .withTableClass("STANDARD")
                .withTags(new Tag().withKey("service").withValue("clubs-service-dynamo"))
                .withAttributeDefinitions(
                        new AttributeDefinition("PK", "S"),
                        new AttributeDefinition("SK", "S"))
                .withKeySchema(
                        new KeySchemaElement("PK", "HASH"),
                        new KeySchemaElement("SK", "RANGE"))
                .withGlobalSecondaryIndexes(new GlobalSecondaryIndex()
                        .withIndexName("Index_SK")
                        .withKeySchema(
                                new KeySchemaElement("SK", "HASH"),
                                new KeySchemaElement("PK", "RANGE"))
                        .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                        .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L)))
                .withBillingMode("PROVISIONED")
                .withProvisionedThroughput(new ProvisionedThroughput(1L, 1L))
                .withStreamSpecification(new StreamSpecification()
                        .withStreamEnabled(true)
                        .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES))
                .withSSESpecification(new SSESpecification()
                        .withEnabled(true)
                        .withSSEType(SSEType.KMS)
                        .withKMSMasterKeyId(""));
        amazonDynamoDB.createTable(createTableRequest);
    }

    @AfterEach
    public void _tearDown() {
        WireMock.resetToDefault();
        amazonDynamoDB.deleteTable("ClubPlayerTable");
    }

    @Autowired
    protected ClubService clubService;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired(required = false)
    protected DynamoDbClient dynamoDbClient;

    @Autowired
    protected DynamoDBMapper dynamoDBMapper;
}
