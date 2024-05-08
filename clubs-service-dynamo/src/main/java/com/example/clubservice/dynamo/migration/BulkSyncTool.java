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
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.ClubPlayerItem;
import com.example.clubservice.dynamo.model.Player;
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
                Club club = new Club(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("country"),
                        resultSet.getString("president"),
                        resultSet.getTimestamp("created"),
                        resultSet.getTimestamp("modified")
                );
                clubItems.add(ClubPlayerItem.fromClub(club));
            }

            resultSet = statement.executeQuery("SELECT * FROM players");
            List<ClubPlayerItem> playerItems = new ArrayList<>();
            while (resultSet.next()) {
                Long clubId = resultSet.getLong("club_id");
                Player player = new Player(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("country"),
                        resultSet.getInt("rating"),
                        resultSet.getTimestamp("created"),
                        resultSet.getTimestamp("modified"),
                        clubId!=0?clubId:null
                );
                playerItems.add(ClubPlayerItem.fromPlayer(player));
            }

            // Initialize DynamoDB client
            DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);

            // Write data to DynamoDB
            TableWriteItems clubWriteItems = new TableWriteItems(DYNAMODB_TABLE_NAME);
            for (ClubPlayerItem club : clubItems) {
                clubWriteItems.addItemToPut(club.toItem());
            }

            TableWriteItems playerWriteItems = new TableWriteItems(DYNAMODB_TABLE_NAME);
            for (ClubPlayerItem player : playerItems) {
                playerWriteItems.addItemToPut(player.toItem());
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
