package com.example.clubservice.dynamo.model;



import java.sql.Timestamp;

public class Player {

    private Long id;

    private String name;

    private String country;
    
    private Integer rating;

    private Timestamp created;

    private Timestamp modified;

    private Long clubId;

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
}