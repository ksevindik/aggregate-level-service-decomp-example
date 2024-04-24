package com.example.clubsmonolith;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;

import javax.sql.DataSource;
import java.sql.SQLException;


@EmbeddedKafka
@SpringBootTest
public class BaseIntegrationTests {
    @Autowired
    private DataSource dataSource;

    public void openDBConsole() {
        try {
            Server.startWebServer(DataSourceUtils.getConnection(dataSource));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
