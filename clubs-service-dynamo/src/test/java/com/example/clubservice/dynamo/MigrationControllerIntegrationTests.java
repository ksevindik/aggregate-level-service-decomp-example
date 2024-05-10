package com.example.clubservice.dynamo;

import com.example.clubservice.dynamo.base.BaseIntegrationTests;
import com.example.clubservice.dynamo.migration.MigrationProperties;
import com.example.clubservice.dynamo.migration.OperationMode;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = "spring.datasource.url=${service.migration.source-db-url}")
public class MigrationControllerIntegrationTests extends BaseIntegrationTests {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public DataSource sourceDataSource(MigrationProperties migrationProperties) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(migrationProperties.getSourceDbUrl());
            dataSource.setUsername(migrationProperties.getSourceDbUsername());
            dataSource.setPassword(migrationProperties.getSourceDbPassword());
            return dataSource;
        }

        @Bean
        public PlatformTransactionManager jdbcTransactionManager(@Qualifier("sourceDataSource") DataSource sourceDataSource) {
            return new JdbcTransactionManager(sourceDataSource);
        }

        @Bean
        @Primary
        public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }

    @Test
    @Sql(scripts = "/bulk-sync-test-data.sql",config = @SqlConfig(dataSource = "sourceDataSource", transactionManager = "jdbcTransactionManager"))
    public void testBulkSync() {
        String url = "/migrations/bulkSync";
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, null, String.class);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertEquals("Bulk sync completed successfully.", responseEntity.getBody());
    }

    @Test
    public void testGetOperationMode() {
        String url = "/migrations/operationMode";
        OperationMode operationMode = restTemplate.getForObject(url, OperationMode.class);
        assertEquals(OperationMode.READ_ONLY, operationMode);
    }

    @Test
    public void testSetOperationMode() {
        String url = "/migrations/operationMode";
        restTemplate.put(url, OperationMode.READ_WRITE);
        OperationMode operationMode = restTemplate.getForObject(url, OperationMode.class);
        assertEquals(OperationMode.READ_WRITE, operationMode);
    }
}
