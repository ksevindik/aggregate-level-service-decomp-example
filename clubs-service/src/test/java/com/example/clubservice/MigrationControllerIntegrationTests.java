package com.example.clubservice;

import com.example.clubservice.base.BaseIntegrationTests;
import com.example.clubservice.migration.MigrationProperties;
import com.example.clubservice.migration.OperationMode;
import com.example.clubservice.repository.ClubRepository;
import com.example.clubservice.repository.PlayerRepository;
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

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public DataSource dataSource(MigrationProperties migrationProperties) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(migrationProperties.getTargetDbUrl());
            dataSource.setUsername(migrationProperties.getTargetDbUsername());
            dataSource.setPassword(migrationProperties.getTargetDbPassword());
            return dataSource;
        }
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
        assertEquals(0, clubRepository.count());
        assertEquals(0, playerRepository.count());
        String url = "/migrations/bulkSync";
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, null, String.class);
        assertEquals(200, responseEntity.getStatusCodeValue());
        assertEquals("Bulk sync completed successfully.", responseEntity.getBody());
        assertEquals(3, clubRepository.count());
        assertEquals(7, playerRepository.count());
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
