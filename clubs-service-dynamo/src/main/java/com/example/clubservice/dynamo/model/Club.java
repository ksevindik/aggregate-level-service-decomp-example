package com.example.clubservice.dynamo.model;

import java.sql.Timestamp;

public class Club {

    private Long id;

    private String name;

    private String country;
    
    private String president;

    private Timestamp created;

    private Timestamp modified;

    public Club(Long id, String name, String country, String president, Timestamp created, Timestamp modified) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.president = president;
        this.created = created;
        this.modified = modified;
    }

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

    public String getPresident() {
        return president;
    }

    public void setPresident(String president) {
        this.president = president;
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
}