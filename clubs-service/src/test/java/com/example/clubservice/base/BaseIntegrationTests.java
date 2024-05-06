package com.example.clubservice.base;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.h2.tools.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;


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
    private RestTemplateBuilder restTemplateBuilder;

    protected RestTemplate restTemplate;

    @BeforeEach
    public void _setUp() {
        restTemplate = restTemplateBuilder.rootUri("http://localhost:" + port).build();
    }

    @AfterEach
    public void _tearDown() {
        WireMock.resetToDefault();
    }

    @BeforeAll
    static void _beforeAll() {
        // Configure WireMock to have a longer shutdown timeout
        //WireMockSpring.options().jettyStopTimeout(100000L).timeout(100000);
    }

    protected void openDBConsole() {
        try {
            Server.startWebServer(DataSourceUtils.getConnection(dataSource));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
