package com.example.clubsmonolith.repository;

import com.example.clubsmonolith.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByCountry(String country);
    List<Player> findByNameContaining(String namePattern);
    List<Player> findByRatingGreaterThanEqual(Integer rating);
    List<Player> findByRatingLessThanEqual(Integer rating);
    
    @Query("SELECT p FROM Player p WHERE p.club.name = :clubName")
    List<Player> findByClubName(String clubName);
}