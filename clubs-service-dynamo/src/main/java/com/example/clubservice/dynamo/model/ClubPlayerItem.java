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
    private Long monolithId;
    private boolean synced;

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

    @DynamoDBAttribute(attributeName = "monolithId")
    public Long getMonolithId() {
        return monolithId;
    }

    public void setMonolithId(Long monolithId) {
        this.monolithId = monolithId;
    }

    @DynamoDBAttribute(attributeName = "synced")
    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public Item toItem() {
        Item item = new Item()
                .withPrimaryKey("PK", PK)
                .withString("SK", SK)
                .withLong("id", id)
                .withString("itemName", name)
                .withString("country", country)
                .withNumber("created", created.getTime())
                .withNumber("modified", modified.getTime())
                .withBoolean("synced", synced);
        if(clubId != null) {
            item.withLong("clubId", clubId);
        }
        if(rating != null) {
            item.withNumber("rating", rating);
        }
        if(president != null) {
            item.withString("president", president);
        }
        if(monolithId!= null) {
            item.withLong("monolithId", monolithId);
        }
        return item;
    }

    public Club toClub() {
        Club club = new Club(id, name, country, president, created, modified);
        club.setSynced(isSynced());
        return club;
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
        item.setSynced(club.isSynced());
        return item;
    }

    public Player toPlayer() {
        Player player = new Player(id, name, country, rating, created, modified, clubId);
        player.setSynced(isSynced());
        return player;
    }

    public void applyChanges(Club monolithClub) {
        this.setName(monolithClub.getName());
        this.setCountry(monolithClub.getCountry());
        this.setPresident(monolithClub.getPresident());
        this.setCreated(monolithClub.getCreated());
        this.setModified(monolithClub.getModified());
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
        item.setSynced(player.isSynced());
        return item;
    }

    @Override
    public String toString() {
        return "ClubPlayerItem{" +
                "PK='" + PK + '\'' +
                ", SK='" + SK + '\'' +
                ", id=" + id +
                ", clubId=" + clubId +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", president='" + president + '\'' +
                ", rating=" + rating +
                ", created=" + created +
                ", modified=" + modified +
                ", monolithId=" + monolithId +
                ", synced=" + synced +
                '}';
    }
}