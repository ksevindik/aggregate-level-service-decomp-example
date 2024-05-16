package com.example.clubservice.dynamo.controller;

import com.example.clubservice.dynamo.migration.ReadWriteApiDispatcher;
import com.example.clubservice.dynamo.model.Club;
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
@RequestMapping("/clubs")
public class ClubController {

    @Autowired
    private ReadWriteApiDispatcher readWriteApiDispatcher;

    @GetMapping
    public ResponseEntity<List<Club>> getAllClubs() {
        return ResponseEntity.ok(readWriteApiDispatcher.getAllClubs());
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Club>> getClubsByCountry(@PathVariable String country) {
        return ResponseEntity.ok(readWriteApiDispatcher.getClubsByCountry(country));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Club> getClubById(@PathVariable Long id) {
        return readWriteApiDispatcher.getClubById(id)
                          .map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Club>> getClubsByNamePattern(@RequestParam String name) {
        return ResponseEntity.ok(readWriteApiDispatcher.getClubsByNamePattern(name));
    }

    @PostMapping
    public ResponseEntity<Club> createClub(@RequestBody Club club) {
        Club createdClub = readWriteApiDispatcher.createClub(club);
        return new ResponseEntity<>(createdClub, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/president")
    public ResponseEntity<Club> updatePresident(@PathVariable Long id, @RequestBody String president) {
        return ResponseEntity.ok(readWriteApiDispatcher.updatePresident(id, president));
    }
}