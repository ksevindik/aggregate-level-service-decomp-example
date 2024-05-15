package com.example.clubservice.dynamo.model;



import java.sql.Timestamp;
import java.util.Objects;

public class Player {

    private Long id;

    private String name;

    private String country;
    
    private Integer rating;

    private Timestamp created;

    private Timestamp modified;

    private Long clubId;

    private boolean synced;

    public Player() {
    }

    public Player(Player player) {
        this(player.getId(), player.getName(), player.getCountry(), player.getRating(), player.getCreated(), player.getModified(), player.getClubId());
    }

    public Player(String name, String country, Integer rating, Long clubId) {
        this(null, name, country, rating, null, null, clubId);
    }

    public Player(Long id, String name, String country, Integer rating, Long clubId) {
        this(id, name, country, rating, null, null, clubId);
    }

    public Player(Long id, String name, String country, Integer rating, Timestamp created, Timestamp modified, Long clubId) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.rating = rating;
        this.created = created;
        this.modified = modified;
        this.clubId = clubId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp getModified() {
        return modified;
    }

    public void setModified(Timestamp modified) {
        this.modified = modified;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id) && Objects.equals(name, player.name) && Objects.equals(country, player.country) && Objects.equals(rating, player.rating) && Objects.equals(created, player.created) && Objects.equals(modified, player.modified) && Objects.equals(clubId, player.clubId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, rating, created, modified, clubId);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", rating=" + rating +
                ", created=" + created +
                ", modified=" + modified +
                ", clubId=" + clubId +
                ", synced=" + synced +
                '}';
    }
}