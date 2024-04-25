package com.example.clubservice;

import com.example.clubservice.migration.OperationModeManager;
import com.example.clubservice.repository.IdMappingRepository;
import org.h2.tools.Server;
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
@EmbeddedKafka
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = "service.migration.monolith-base-url=http://localhost:${wiremock.server.port}")
public class BaseIntegrationTests {
    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private Long port;

    @Autowired
    protected OperationModeManager operationModeManager;

    @Autowired
    protected IdMappingRepository idMappingRepository;

    protected RestTemplate restTemplate;

    @BeforeEach
    public void _setUp() {
        restTemplate = restTemplateBuilder.rootUri("http://localhost:" + port).build();
    }

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    public void openDBConsole() {
        try {
            Server.startWebServer(DataSourceUtils.getConnection(dataSource));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
