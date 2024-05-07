package com.example.clubservice.dynamo.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BulkSyncTool {

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    private MigrationProperties migrationProperties;

    private static final String DYNAMODB_TABLE_NAME = "ClubPlayerTable";

    public void bulkSync() {
        try {
            // Connect to MySQL
            Connection mysqlConnection = DriverManager.getConnection(
                    migrationProperties.getSourceDbUrl(),
                    migrationProperties.getSourceDbUsername(),
                    migrationProperties.getSourceDbPassword());
            Statement statement = mysqlConnection.createStatement();

            // Query data from MySQL
            ResultSet resultSet = statement.executeQuery("SELECT * FROM clubs");
            List<ClubPlayerItem> clubItems = new ArrayList<>();
            while (resultSet.next()) {
                ClubPlayerItem club = new ClubPlayerItem(
                        resultSet.getLong("id"),
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("country"),
                        resultSet.getString("president"),
                        0,
                        resultSet.getTimestamp("created"),
                        resultSet.getTimestamp("modified")
                );
                clubItems.add(club);
            }

            resultSet = statement.executeQuery("SELECT * FROM players");
            List<ClubPlayerItem> playerItems = new ArrayList<>();
            while (resultSet.next()) {
                ClubPlayerItem player = new ClubPlayerItem(
                        resultSet.getLong("id"),
                        resultSet.getLong("club_id"),
                        resultSet.getString("name"),
                        resultSet.getString("country"),
                        "",
                        resultSet.getInt("rating"),
                        resultSet.getTimestamp("created"),
                        resultSet.getTimestamp("modified")
                );
                playerItems.add(player);
            }

            // Initialize DynamoDB client
            DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);

            // Write data to DynamoDB
            TableWriteItems clubWriteItems = new TableWriteItems(DYNAMODB_TABLE_NAME);
            for (ClubPlayerItem club : clubItems) {
                clubWriteItems.addItemToPut(new PutItemSpec()
                        .withItem(new Item()
                                .withPrimaryKey("PK", "CLUB#" + club.getId())
                                .withString("SK", "CLUB#" + club.getId())
                                .withLong("id", club.getId())
                                .withString("name", club.getName())
                                .withString("country", club.getCountry())
                                .withString("president", club.getPresident())
                                .withNumber("rating", club.getRating())
                                .withNumber("created", club.getCreated().getTime())
                                .withNumber("modified", club.getModified().getTime())
                        ).getItem()
                );
            }

            TableWriteItems playerWriteItems = new TableWriteItems(DYNAMODB_TABLE_NAME);
            for (ClubPlayerItem player : playerItems) {
                playerWriteItems.addItemToPut(new PutItemSpec()
                        .withItem(new Item()
                                .withPrimaryKey("PK", "CLUB#" + player.getClubId())
                                .withString("SK", "PLAYER#" + player.getId())
                                .withLong("id", player.getId())
                                .withLong("clubId", player.getClubId())
                                .withString("name", player.getName())
                                .withString("country", player.getCountry())
                                .withString("president", player.getPresident())
                                .withInt("rating", player.getRating())
                                .withNumber("created", player.getCreated().getTime())
                                .withNumber("modified", player.getModified().getTime())
                        ).getItem()
                );
            }

            // Batch write to DynamoDB
            // Separate batch write requests for clubs and players
            dynamoDB.batchWriteItem(clubWriteItems);
            dynamoDB.batchWriteItem(playerWriteItems);

            // Close connections
            statement.close();
            mysqlConnection.close();

            System.out.println("Data migration completed successfully.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
