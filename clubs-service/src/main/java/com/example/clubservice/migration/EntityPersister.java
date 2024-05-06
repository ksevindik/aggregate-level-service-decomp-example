package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.ClubRepository;
import com.example.clubservice.repository.IdMappingRepository;
import com.example.clubservice.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EntityPersister {
    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private IdMappingRepository idMappingRepository;

    @Autowired
    private MonolithReadWriteApiAdapter monolithReadWriteApiAdapter;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public Club createFrom(Club monolithClub) {
        Club serviceClub = new Club();
        applyChanges(monolithClub, serviceClub);
        serviceClub = clubRepository.save(serviceClub);
        idMappingRepository.save(new IdMapping(serviceClub.getId(), monolithClub.getId(),"Club"));
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClub));
        return serviceClub;
    }

    public Club updateFrom(Club monolithClub) {
        Long serviceClubId = resolveServiceClubId(monolithClub.getId());
        Club serviceClub = clubRepository.findById(serviceClubId).orElseThrow(()->new RuntimeException("Club not found with id: " + serviceClubId));
        applyChanges(monolithClub, serviceClub);
        serviceClub = clubRepository.save(serviceClub);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClub));
        return serviceClub;
    }

    public void deleteFrom(Club monolithClub) {
        Long serviceClubId = resolveServiceClubId(monolithClub.getId());
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClubId));
        clubRepository.deleteById(serviceClubId);
    }

    public Player createFrom(Player monolithPlayer) {
        Player servicePlayer = new Player();
        applyChanges(monolithPlayer, servicePlayer);
        servicePlayer = playerRepository.save(servicePlayer);
        idMappingRepository.save(new IdMapping(servicePlayer.getId(), monolithPlayer.getId(), "Player"));
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayer));
        return servicePlayer;
    }

    public Player updateFrom(Player monolithPlayer) {
        Long servicePlayerId = resolveServicePlayerId(monolithPlayer.getId());
        Player servicePlayer = playerRepository.findById(servicePlayerId).orElseThrow(()->new RuntimeException("Player not found with id: " + servicePlayerId));
        applyChanges(monolithPlayer, servicePlayer);
        servicePlayer = playerRepository.save(servicePlayer);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayer));
        return servicePlayer;
    }

    public void deleteFrom(Player monolithPlayer) {
        Long servicePlayerId = resolveServicePlayerId(monolithPlayer.getId());
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayerId));
        playerRepository.deleteById(servicePlayerId);
    }

    public void syncClubEntityIfNecessary(Long clubId) {
        Club club = clubRepository.findById(clubId).orElse(null);
        if(club == null || !club.isSynced()) {
            Long monolithClubId = idMappingRepository.findByServiceIdAndTypeName(clubId, "Club").getMonolithId();
            club = monolithReadWriteApiAdapter.getClubById(monolithClubId);
            club = club== null ? this.createFrom(club): this.updateFrom(club);
            club.setSynced(true);
        }
    }

    public void syncPlayerEntityIfNecessary(Long playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if(player == null || !player.isSynced()) {
            Long monolithPlayerId = idMappingRepository.findByServiceIdAndTypeName(playerId, "Player").getMonolithId();
            player = monolithReadWriteApiAdapter.getPlayerById(monolithPlayerId);
            player = player== null ? this.createFrom(player): this.updateFrom(player);
            player.setSynced(true);
        }
    }


    private void applyChanges(Club monolithClub, Club serviceClub) {
        serviceClub.setName(monolithClub.getName());
        serviceClub.setCountry(monolithClub.getCountry());
        serviceClub.setPresident(monolithClub.getPresident());
        serviceClub.setCreated(monolithClub.getCreated());
        serviceClub.setModified(monolithClub.getModified());
    }

    private void applyChanges(Player monolithPlayer, Player servicePlayer) {
        servicePlayer.setName(monolithPlayer.getName());
        servicePlayer.setCountry(monolithPlayer.getCountry());
        servicePlayer.setRating(monolithPlayer.getRating());
        servicePlayer.setCreated(monolithPlayer.getCreated());
        servicePlayer.setModified(monolithPlayer.getModified());
        if(monolithPlayer.getClubId() != null) {
            Long serviceClubId = resolveServiceClubId(monolithPlayer.getClubId());
            Club serviceClub = clubRepository.findById(serviceClubId).orElseThrow();
            servicePlayer.setClub(serviceClub);
        }
    }

    private Long resolveServiceClubId(Long clubId) {
        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(clubId, "Club");
        return idMapping != null ? idMapping.getServiceId() : clubId;
    }

    private Long resolveServicePlayerId(Long playerId) {
        IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(playerId, "Player");
        return idMapping != null ? idMapping.getServiceId() : playerId;
    }
}
