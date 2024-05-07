package com.example.clubservice.dynamo.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.example.clubservice.dynamo.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;


    public List<Player> getAllPlayers() {
        return List.of();
    }


    public List<Player> getPlayersByClubName(String clubName) {
        return List.of();
    }


    public List<Player> getPlayersByCountry(String country) {
        return List.of();
    }


    public Optional<Player> getPlayerById(Long id) {
        return Optional.ofNullable(null);
    }


    public List<Player> getPlayersByNamePattern(String namePattern) {
        return List.of();
    }


    public List<Player> getPlayersByRatingGreaterThanOrEqual(Integer rating) {
        return List.of();
    }


    public List<Player> getPlayersByRatingLessThanOrEqual(Integer rating) {
        return List.of();
    }


    public Player createPlayer(Player player) {
        return player;
    }


    public Player updatePlayerRating(Long playerId, Integer rating) {
        return null;
    }


    public Player transferPlayer(Long playerId, Long clubId) {
        return null;
    }
}