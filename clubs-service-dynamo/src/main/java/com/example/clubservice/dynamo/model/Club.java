package com.example.clubservice.dynamo.model;

import java.sql.Timestamp;
import java.util.Objects;

public class Club {

    private Long id;

    private String name;

    private String country;
    
    private String president;

    private Timestamp created;

    private Timestamp modified;

    private boolean synced;

    private Long monolithId;


    public Club() {
    }

    public Club(Club club) {
        this(club.getId(), club.getName(), club.getCountry(), club.getPresident(), club.getCreated(), club.getModified());
        this.setSynced(club.isSynced());
        this.setMonolithId(club.getMonolithId());
    }

    public Club(String name, String country, String president) {
        this(null, name, country, president, null, null);
    }

    public Club(Long id, String name, String country, String president) {
        this(id, name, country, president, null, null);
    }

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


    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public Long getMonolithId() {
        return monolithId;
    }

    public void setMonolithId(Long monolithId) {
        this.monolithId = monolithId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Club club = (Club) o;
        return Objects.equals(id, club.id) && Objects.equals(name, club.name) && Objects.equals(country, club.country)
                && Objects.equals(president, club.president) && Objects.equals(created, club.created)
                && Objects.equals(modified, club.modified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, president, created, modified);
    }

    @Override
    public String toString() {
        return "Club{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", president='" + president + '\'' +
                ", created=" + created +
                ", modified=" + modified +
                ", synced=" + synced +
                '}';
    }
}