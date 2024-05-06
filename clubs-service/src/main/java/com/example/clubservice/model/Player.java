package com.example.clubservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "players")
@EntityListeners(AuditingEntityListener.class)
public class Player {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String country;
    
    private Integer rating;
    
    @Column(nullable = false)
    @CreatedDate
    private Timestamp created;
    
    @Column(nullable = false)
    @LastModifiedDate
    private Timestamp modified;
    
    @ManyToOne
    @JoinColumn(name = "club_id", referencedColumnName = "id")
    @JsonIgnore
    private Club club;

    @Column(name="club_id", insertable = false, updatable = false)
    private Long clubId;

    private boolean synced;

    public Player() {
    }

    public Player(Long id, String name, String country, Integer rating, Club club) {
        this(name, country, rating, club);
        this.id = id;
    }

    public Player(String name, String country, Integer rating, Club club) {
        this.name = name;
        this.country = country;
        this.rating = rating;
        this.setClub(club);
    }

    public Player(Player copy) {
        this(copy.getId(), copy.getName(), copy.getCountry(), copy.getRating(), copy.getClub());
        this.setCreated(copy.getCreated());
        this.setModified(copy.getModified());
        this.setSynced(copy.isSynced());
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
    
    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
        if(club != null) {
            setClubId(club.getId());
        }
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
        return Objects.equals(name, player.name) && Objects.equals(country, player.country) && Objects.equals(rating, player.rating) && Objects.equals(clubId, player.clubId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, country, rating, clubId);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", rating=" + rating +
                ", clubId=" + clubId +
                ", synced=" + synced +
                '}';
    }
}