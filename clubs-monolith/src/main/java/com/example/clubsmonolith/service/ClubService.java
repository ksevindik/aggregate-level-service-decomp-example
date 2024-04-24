package com.example.clubsmonolith.service;

import com.example.clubsmonolith.migration.EntityChangeEventPublisher;
import com.example.clubsmonolith.model.Club;
import com.example.clubsmonolith.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClubService {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private EntityChangeEventPublisher entityChangeEventPublisher;

    @Transactional(readOnly = true)
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Club> getClubsByCountry(String country) {
        return clubRepository.findByCountry(country);
    }

    @Transactional(readOnly = true)
    public Optional<Club> getClubById(Long id) {
        return clubRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Club> getClubsByNamePattern(String namePattern) {
        return clubRepository.findByNameContaining(namePattern);
    }

    @Transactional
    public Club createClub(Club club) {
        Club savedClub = clubRepository.save(club);
        entityChangeEventPublisher.publishClubEvent(savedClub, "CREATE");
        return savedClub;
    }

    @Transactional
    public Club updatePresident(Long clubId, String president) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new RuntimeException("Club not found"));
        club.setPresident(president);
        Club updatedClub = clubRepository.save(club);
        entityChangeEventPublisher.publishClubEvent(updatedClub, "UPDATE");
        return updatedClub;
    }
}