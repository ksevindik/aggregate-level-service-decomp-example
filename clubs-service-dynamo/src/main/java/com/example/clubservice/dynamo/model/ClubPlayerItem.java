package com.example.clubservice.dynamo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.document.Item;

import java.sql.Timestamp;


@DynamoDBTable(tableName = "ClubPlayerTable")
public class ClubPlayerItem {

    private String PK;
    private String SK;
    private Long id;
    private Long clubId;
    private String name;
    private String country;
    private String president;
    private Integer rating;
    private Timestamp created;
    private Timestamp modified;

    public ClubPlayerItem() {
    }

    public ClubPlayerItem(Long id, Long clubId, String name,
                          String country, String president, Integer rating,
                          Timestamp created, Timestamp modified) {
        this.id = id;
        this.clubId = clubId;
        this.name = name;
        this.country = country;
        this.president = president;
        this.rating = rating;
        this.created = created;
        this.modified = modified;
    }

    @DynamoDBHashKey(attributeName = "PK")
    public String getPK() {
        return PK;
    }

    public void setPK(String PK) {
        this.PK = PK;
    }

    @DynamoDBRangeKey(attributeName = "SK")
    public String getSK() {
        return SK;
    }

    public void setSK(String SK) {
        this.SK = SK;
    }

    @DynamoDBAttribute(attributeName = "id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @DynamoDBAttribute(attributeName = "clubId")
    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    @DynamoDBAttribute(attributeName = "itemName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBAttribute(attributeName = "country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @DynamoDBAttribute(attributeName = "president")
    public String getPresident() {
        return president;
    }

    public void setPresident(String president) {
        this.president = president;
    }

    @DynamoDBAttribute(attributeName = "rating")
    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    @DynamoDBAttribute(attributeName = "created")
    @DynamoDBTypeConverted(converter = TimestampConverter.class)
    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    @DynamoDBAttribute(attributeName = "modified")
    @DynamoDBTypeConverted(converter = TimestampConverter.class)
    public Timestamp getModified() {
        return modified;
    }

    public void setModified(Timestamp modified) {
        this.modified = modified;
    }

    public Item toItem() {
        Item item = new Item()
                .withPrimaryKey("PK", PK)
                .withString("SK", SK)
                .withLong("id", id)
                .withString("itemName", name)
                .withString("country", country)
                .withNumber("created", created.getTime())
                .withNumber("modified", modified.getTime());
        if(clubId != null) {
            item.withLong("clubId", clubId);
        }
        if(rating != null) {
            item.withNumber("rating", rating);
        }
        if(president != null) {
            item.withString("president", president);
        }
        return item;
    }

    public Club toClub() {
        return new Club(id, name, country, president, created, modified);
    }

    public static ClubPlayerItem fromClub(Club club) {
        ClubPlayerItem item =  new ClubPlayerItem(
                club.getId(),
                null,
                club.getName(),
                club.getCountry(),
                club.getPresident(),
                null,
                club.getCreated(),
                club.getModified());
        item.setPK("CLUB#" + club.getId());
        item.setSK("CLUB#" + club.getId());
        return item;
    }

    public Player toPlayer() {
        return new Player(id, name, country, rating, created, modified, clubId);
    }

    public static ClubPlayerItem fromPlayer(Player player) {
        ClubPlayerItem item = new ClubPlayerItem(
                player.getId(),
                player.getClubId(),
                player.getName(),
                player.getCountry(),
                null,
                player.getRating(),
                player.getCreated(),
                player.getModified());
        item.setPK("CLUB#" + (player.getClubId()!=null?player.getClubId():0));
        item.setSK("PLAYER#" + player.getId());
        return item;
    }
}