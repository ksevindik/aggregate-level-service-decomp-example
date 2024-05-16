package com.example.clubservice.dynamo.controller;

import com.example.clubservice.dynamo.migration.ReadWriteApiDispatcher;
import com.example.clubservice.dynamo.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/players")
public class PlayerController {

    @Autowired
    private ReadWriteApiDispatcher readWriteApiDispatcher;

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(readWriteApiDispatcher.getAllPlayers());
    }

    @GetMapping("/clubName")
    public ResponseEntity<List<Player>> getPlayersByClubName(@RequestParam String clubName) {
        return ResponseEntity.ok(readWriteApiDispatcher.getPlayersByClubName(clubName));
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Player>> getPlayersByCountry(@PathVariable String country) {
        return ResponseEntity.ok(readWriteApiDispatcher.getPlayersByCountry(country));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable Long id) {
        return readWriteApiDispatcher.getPlayerById(id)
                            .map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Player>> getPlayersByNamePattern(@RequestParam String name) {
        return ResponseEntity.ok(readWriteApiDispatcher.getPlayersByNamePattern(name));
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        Player createdPlayer = readWriteApiDispatcher.createPlayer(player);
        return new ResponseEntity<>(createdPlayer, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<Player> updatePlayerRating(@PathVariable Long id, @RequestBody Integer rating) {
        return ResponseEntity.ok(readWriteApiDispatcher.updateRating(id, rating));
    }

    @PutMapping("/{id}/transfer")
    public ResponseEntity<Player> transferPlayer(@PathVariable Long id, @RequestBody Long clubId) {
        return ResponseEntity.ok(readWriteApiDispatcher.transferPlayer(id, clubId));
    }
}