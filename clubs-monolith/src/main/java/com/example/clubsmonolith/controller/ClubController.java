package com.example.clubsmonolith.controller;

import com.example.clubsmonolith.model.Club;
import com.example.clubsmonolith.service.ClubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs")
public class ClubController {

    @Autowired
    private ClubService clubService;

    @GetMapping
    public ResponseEntity<List<Club>> getAllClubs() {
        return ResponseEntity.ok(clubService.getAllClubs());
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Club>> getClubsByCountry(@PathVariable String country) {
        return ResponseEntity.ok(clubService.getClubsByCountry(country));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Club> getClubById(@PathVariable Long id) {
        return clubService.getClubById(id)
                          .map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Club>> getClubsByNamePattern(@RequestParam String name) {
        return ResponseEntity.ok(clubService.getClubsByNamePattern(name));
    }

    @PostMapping
    public ResponseEntity<Club> createClub(@RequestBody Club club) {
        Club createdClub = clubService.createClub(club);
        return new ResponseEntity<>(createdClub, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/president")
    public ResponseEntity<Club> updatePresident(@PathVariable Long id, @RequestBody String president) {
        return ResponseEntity.ok(clubService.updatePresident(id, president));
    }
}