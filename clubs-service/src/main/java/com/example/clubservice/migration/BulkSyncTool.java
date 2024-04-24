package com.example.clubservice.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class BulkSyncTool {

    @Autowired
    private MigrationProperties migrationProperties;


    public void bulkSync() {
        syncClubs();
        syncPlayers();
    }

    private void syncClubs() {
        String query = "SELECT * FROM clubs;";
        String insert = "INSERT INTO clubs (name, country, president, created, modified) VALUES (?, ?, ?, ?, ?);";
        String insertIdMapping = "INSERT INTO id_mappings (service_id, monolith_id, type_name) VALUES (?, ?, ?);";

        try (Connection sourceCon = DriverManager.getConnection(
                migrationProperties.getSourceDbUrl(),
                migrationProperties.getSourceDbUsername(),
                migrationProperties.getSourceDbPassword());
             Connection targetCon = DriverManager.getConnection(
                     migrationProperties.getTargetDbUrl(),
                     migrationProperties.getTargetDbUsername(),
                     migrationProperties.getTargetDbPassword());
             PreparedStatement selectStmt = sourceCon.prepareStatement(query);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement insertIdMappingStmt = targetCon.prepareStatement(insertIdMapping);
             PreparedStatement insertStmt = targetCon.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {
                while (rs.next()) {
                    insertStmt.setString(1, rs.getString("name"));
                    insertStmt.setString(2, rs.getString("country"));
                    insertStmt.setString(3, rs.getString("president"));
                    insertStmt.setTimestamp(4, rs.getTimestamp("created"));
                    insertStmt.setTimestamp(5, rs.getTimestamp("modified"));
                    insertStmt.executeUpdate();

                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    generatedKeys.next();
                    Long clubId = generatedKeys.getLong(1);

                    insertIdMappingStmt.setLong(1, clubId);
                    insertIdMappingStmt.setLong(2, rs.getLong("id"));
                    insertIdMappingStmt.setString(3, "Club");

                    insertIdMappingStmt.executeUpdate();
                }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void syncPlayers() {
        String query = "SELECT * FROM players;";
        String queryToSelectIdMapping = "SELECT service_id FROM id_mappings WHERE monolith_id = ? AND type_name = 'Club';";
        String insert = "INSERT INTO players (club_id, name, country, rating, created, modified) VALUES (?, ?, ?, ?, ?, ?);";
        String insertIdMapping = "INSERT INTO id_mappings (service_id, monolith_id, type_name) VALUES (?, ?, ?);";

        try (Connection sourceCon = DriverManager.getConnection(
                migrationProperties.getSourceDbUrl(),
                migrationProperties.getSourceDbUsername(),
                migrationProperties.getSourceDbPassword());
             Connection targetCon = DriverManager.getConnection(
                     migrationProperties.getTargetDbUrl(),
                     migrationProperties.getTargetDbUsername(),
                     migrationProperties.getTargetDbPassword());
             PreparedStatement selectStmt = sourceCon.prepareStatement(query);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement queryToSelectIdMappingStmt = targetCon.prepareStatement(queryToSelectIdMapping);
             PreparedStatement insertIdMappingStmt = targetCon.prepareStatement(insertIdMapping);
             PreparedStatement insertStmt = targetCon.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS)) {

            while (rs.next()) {
                if(rs.getObject("club_id") != null) {
                    Long monolithClubId = rs.getLong("club_id");
                    queryToSelectIdMappingStmt.setLong(1, monolithClubId);
                    ResultSet rs2 = queryToSelectIdMappingStmt.executeQuery();
                    rs2.next();
                    Long clubId = rs2.getLong("service_id");
                    insertStmt.setLong(1, clubId);
                } else {
                    insertStmt.setNull(1, java.sql.Types.BIGINT);
                }
                insertStmt.setString(2, rs.getString("name"));
                insertStmt.setString(3, rs.getString("country"));
                insertStmt.setInt(4, rs.getInt("rating"));
                insertStmt.setTimestamp(5, rs.getTimestamp("created"));
                insertStmt.setTimestamp(6, rs.getTimestamp("modified"));
                insertStmt.executeUpdate();

                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                generatedKeys.next();
                Long playerId = generatedKeys.getLong(1);

                insertIdMappingStmt.setLong(1, playerId);
                insertIdMappingStmt.setLong(2, rs.getLong("id"));
                insertIdMappingStmt.setString(3, "Player");

                insertIdMappingStmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
