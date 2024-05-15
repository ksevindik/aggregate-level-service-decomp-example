package com.example.clubservice.dynamo.service;

import com.example.clubservice.dynamo.migration.EntityChangeEventPublisher;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClubService {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private EntityChangeEventPublisher entityChangeEventPublisher;


    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    public List<Club> getClubsByCountry(String country) {
        return clubRepository.findByCountry(country);
    }

    public Optional<Club> getClubById(Long id) {
        return clubRepository.findById(id);
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        return clubRepository.findByNamePattern(namePattern);
    }

    public Club createClub(Club club) {
        club.setId(System.currentTimeMillis());
        club.setSynced(true);
        clubRepository.save(club);
        entityChangeEventPublisher.publishClubEvent(club, "CREATE");
        return club;
    }

    public Club updatePresident(Long clubId, String president) {
        Club club = clubRepository.findById(clubId).orElseThrow(()-> new RuntimeException("Club not found with id :" + clubId));
        club.setPresident(president);
        clubRepository.save(club);
        entityChangeEventPublisher.publishClubEvent(club, "UPDATE");
        return club;
    }
}