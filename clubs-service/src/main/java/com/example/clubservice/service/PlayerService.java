package com.example.clubservice.service;

import com.example.clubservice.migration.EntityChangeEventPublisher;
import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.ClubRepository;
import com.example.clubservice.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private EntityChangeEventPublisher entityChangeEventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByClubName(String clubName) {
        return playerRepository.findByClubName(clubName);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByCountry(String country) {
        return playerRepository.findByCountry(country);
    }

    @Transactional(readOnly = true)
    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByNamePattern(String namePattern) {
        return playerRepository.findByNameContaining(namePattern);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByRatingGreaterThanOrEqual(Integer rating) {
        return playerRepository.findByRatingGreaterThanEqual(rating);
    }

    @Transactional(readOnly = true)
    public List<Player> getPlayersByRatingLessThanOrEqual(Integer rating) {
        return playerRepository.findByRatingLessThanEqual(rating);
    }

    @Transactional
    public Player createPlayer(Player player) {
        if(player.getClubId() != null) {
            Club club = clubRepository.findById(player.getClubId()).orElseThrow(() -> new RuntimeException("Club not found with id :" + player.getClubId()));
            player.setClub(club);
        }
        Player savedPlayer = playerRepository.save(player);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                entityChangeEventPublisher.publishPlayerEvent(savedPlayer, "CREATE");
            }
        });
        return savedPlayer;
    }

    @Transactional
    public Player updatePlayerRating(Long playerId, Integer rating) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new RuntimeException("Player not found"));
        player.setRating(rating);
        Player updatedPlayer = playerRepository.save(player);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                entityChangeEventPublisher.publishPlayerEvent(updatedPlayer, "UPDATE");
            }
        });
        return updatedPlayer;
    }

    @Transactional
    public Player transferPlayer(Long playerId, Long clubId) {
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new RuntimeException("Player not found with id :" + playerId));
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new RuntimeException("Club not found with id :" + clubId));
        player.setClub(club);
        Player updatedPlayer = playerRepository.save(player);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                entityChangeEventPublisher.publishPlayerEvent(updatedPlayer, "UPDATE");
            }
        });
        return updatedPlayer;
    }
}