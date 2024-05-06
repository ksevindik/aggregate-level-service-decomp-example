package com.example.clubsmonolith.migration;

import com.example.clubsmonolith.model.Club;
import com.example.clubsmonolith.model.IdMapping;
import com.example.clubsmonolith.model.Player;
import com.example.clubsmonolith.repository.ClubRepository;
import com.example.clubsmonolith.repository.IdMappingRepository;
import com.example.clubsmonolith.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
the job of entity persister is to reflect changes received as entity change events sent from the service side to the
monolith DB directly, without triggering any business logic processing.
 */
@Component
@Transactional
public class EntityPersister {
    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private IdMappingRepository idMappingRepository;

    public void createFrom(Club serviceClub) {
        Club monolithClub = new Club();
        applyChanges(serviceClub, monolithClub);
        monolithClub = clubRepository.save(monolithClub);
        idMappingRepository.save(new IdMapping(serviceClub.getId(), monolithClub.getId(),"Club"));
    }

    public void updateFrom(Club serviceClub) {
        Long monolithClubId = resolveMonolithClubId(serviceClub.getId());
        Club monolithClub = clubRepository.findById(monolithClubId).orElseThrow();
        applyChanges(serviceClub, monolithClub);
        clubRepository.save(monolithClub);
    }

    public void deleteFrom(Club serviceClub) {
        Long monolithClubId = resolveMonolithClubId(serviceClub.getId());
        clubRepository.deleteById(monolithClubId);
    }

    public void createFrom(Player servicePlayer) {
        Player monolithPlayer = new Player();
        applyChanges(servicePlayer, monolithPlayer);
        monolithPlayer = playerRepository.save(monolithPlayer);
        idMappingRepository.save(new IdMapping(servicePlayer.getId(), monolithPlayer.getId(),"Player"));
    }

    public void updateFrom(Player servicePlayer) {
        Player monolithPlayer;
        Long monolithPlayerId = resolveMonolithPlayerId(servicePlayer.getId());
        monolithPlayer = playerRepository.findById(monolithPlayerId).orElseThrow();
        applyChanges(servicePlayer, monolithPlayer);
        playerRepository.save(monolithPlayer);
    }

    public void deleteFrom(Player servicePlayer) {
        Long monolithPlayerId;
        monolithPlayerId = resolveMonolithPlayerId(servicePlayer.getId());
        playerRepository.deleteById(monolithPlayerId);
    }

    private void applyChanges(Club serviceClub, Club monolithClub) {
        monolithClub.setName(serviceClub.getName());
        monolithClub.setCountry(serviceClub.getCountry());
        monolithClub.setPresident(serviceClub.getPresident());
        monolithClub.setCreated(serviceClub.getCreated());
        monolithClub.setModified(serviceClub.getModified());
    }

    private void applyChanges(Player servicePlayer, Player monolithPlayer) {
        monolithPlayer.setName(servicePlayer.getName());
        monolithPlayer.setCountry(servicePlayer.getCountry());
        monolithPlayer.setRating(servicePlayer.getRating());
        monolithPlayer.setCreated(servicePlayer.getCreated());
        monolithPlayer.setModified(servicePlayer.getModified());
        if(servicePlayer.getClubId() != null) {
            Long monolithClubId = resolveMonolithClubId(servicePlayer.getClubId());
            Club monolithClub = clubRepository.findById(monolithClubId).orElseThrow();
            monolithPlayer.setClub(monolithClub);
        }
    }

    private Long resolveMonolithClubId(Long clubId) {
        IdMapping idMapping = idMappingRepository.findByServiceIdAndTypeName(clubId, "Club");
        return idMapping != null ? idMapping.getMonolithId() : clubId;
    }

    private Long resolveMonolithPlayerId(Long playerId) {
        IdMapping idMapping = idMappingRepository.findByServiceIdAndTypeName(playerId, "Player");
        return idMapping != null ? idMapping.getMonolithId() : playerId;
    }
}
