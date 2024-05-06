package com.example.clubsmonolith.controller;

import com.example.clubsmonolith.model.Player;
import com.example.clubsmonolith.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/players")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    @GetMapping("/clubName")
    public ResponseEntity<List<Player>> getPlayersByClubName(@RequestParam String clubName) {
        return ResponseEntity.ok(playerService.getPlayersByClubName(clubName));
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Player>> getPlayersByCountry(@PathVariable String country) {
        return ResponseEntity.ok(playerService.getPlayersByCountry(country));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable Long id) {
        return playerService.getPlayerById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Player>> getPlayersByNamePattern(@RequestParam String name) {
        return ResponseEntity.ok(playerService.getPlayersByNamePattern(name));
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        Player createdPlayer = playerService.createPlayer(player);
        return new ResponseEntity<>(createdPlayer, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<Player> updatePlayerRating(@PathVariable Long id, @RequestBody Integer rating) {
        return ResponseEntity.ok(playerService.updatePlayerRating(id, rating));
    }

    @PutMapping("/{id}/transfer")
    public ResponseEntity<Player> transferPlayer(@PathVariable Long id, @RequestBody Long clubId) {
        return ResponseEntity.ok(playerService.transferPlayer(id, clubId));
    }
}