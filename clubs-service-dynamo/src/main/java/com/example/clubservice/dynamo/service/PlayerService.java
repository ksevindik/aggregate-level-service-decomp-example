package com.example.clubservice.dynamo.service;

import com.example.clubservice.dynamo.migration.EntityChangeEventPublisher;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.Player;
import com.example.clubservice.dynamo.repository.ClubRepository;
import com.example.clubservice.dynamo.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }


    public List<Player> getPlayersByClubName(String clubName) {
        return playerRepository.findByClubName(clubName);
    }


    public List<Player> getPlayersByCountry(String country) {
        return playerRepository.findByCountry(country);
    }


    public Optional<Player> getPlayerById(Long id) {
        return playerRepository.findrById(id);
    }


    public List<Player> getPlayersByNamePattern(String namePattern) {
        return playerRepository.findByNamePattern(namePattern);
    }

    public Player createPlayer(Player player) {
        player.setId(System.currentTimeMillis());
        player.setSynced(true);
        playerRepository.save(player);
        entityChangeEventPublisher.publishPlayerEvent(player, "CREATE");
        return player;
    }


    public Player updatePlayerRating(Long playerId, Integer rating) {
        Player player = playerRepository.findrById(playerId).orElseThrow(() -> new RuntimeException("Player not found with id :" + playerId));
        player.setRating(rating);
        playerRepository.save(player);
        entityChangeEventPublisher.publishPlayerEvent(player, "UPDATE");
        return player;
    }


    public Player transferPlayer(Long playerId, Long clubId) {
        Player player = playerRepository.findrById(playerId).orElseThrow(() -> new RuntimeException("Player not found with id :" + playerId));
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new RuntimeException("Club not found with id :" + clubId));
        player.setClubId(club.getId());
        playerRepository.save(player);
        entityChangeEventPublisher.publishPlayerEvent(player, "UPDATE");
        return player;
    }
}