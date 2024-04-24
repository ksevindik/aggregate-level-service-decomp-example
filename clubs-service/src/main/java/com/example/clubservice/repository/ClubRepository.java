package com.example.clubservice.repository;

import com.example.clubservice.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findByCountry(String country);
    List<Club> findByNameContaining(String namePattern);
}