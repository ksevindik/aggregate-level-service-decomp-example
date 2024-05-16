package com.example.clubservice.dynamo.migration;


import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.Player;
import com.example.clubservice.dynamo.repository.ClubRepository;
import com.example.clubservice.dynamo.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/*
the job of entity persister is to reflect changes received as entity change events sent from the monolith side to the
service DB directly, without triggering any business logic processing.
 */
@Component
@Transactional
public class EntityPersister {

    @Autowired
    private MonolithReadWriteApiAdapter monolithReadWriteApiAdapter;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PlayerRepository playerRepository;

    public Club createFrom(Club monolithClub) {
        Club serviceClub = new Club();
        applyChanges(monolithClub, serviceClub);
        serviceClub.setId(System.currentTimeMillis());
        clubRepository.save(serviceClub);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClub));
        return serviceClub;
    }

    private Club findClubById(Long id) {
        return clubRepository.findById(id).orElseThrow(() -> new RuntimeException("Club not found with id: " + id));
    }

    private Player findPlayerById(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
    }

    public Club updateFrom(Club monolithClub) {
        Long serviceClubId = resolveServiceClubId(monolithClub.getId());
        Club serviceClub = findClubById(serviceClubId);
        applyChanges(monolithClub, serviceClub);
        clubRepository.save(serviceClub);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, serviceClub));
        return serviceClub;
    }

    public void deleteFrom(Club monolithClub) {
        Long serviceClubId = resolveServiceClubId(monolithClub.getId());
        Club club = findClubById(serviceClubId);
        clubRepository.delete(club);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, club));
    }

    public Player createFrom(Player monolithPlayer) {
        Player servicePlayer = new Player();
        applyChanges(monolithPlayer, servicePlayer);
        playerRepository.save(servicePlayer);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayer));
        return servicePlayer;
    }

    public Player updateFrom(Player monolithPlayer) {
        Long servicePlayerId = resolveServicePlayerId(monolithPlayer.getId());
        Player servicePlayer = findPlayerById(servicePlayerId);
        applyChanges(monolithPlayer, servicePlayer);
        playerRepository.save(servicePlayer);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayer));
        return servicePlayer;
    }

    public void deleteFrom(Player monolithPlayer) {
        Long servicePlayerId = resolveServicePlayerId(monolithPlayer.getId());
        Player player = findPlayerById(servicePlayerId);
        playerRepository.delete(player);
        applicationEventPublisher.publishEvent(new EntityPersistedEvent(this, servicePlayerId));
    }

    public void syncClubEntityIfNecessary(Long clubId) {
        Club club = clubRepository.findById(clubId).orElse(null);
        if(club != null && club.isSynced()) return;
        if(club == null) {
            Club monolithClub = monolithReadWriteApiAdapter.getClubById(clubId);
            club = new Club(monolithClub);
            club.setId(System.currentTimeMillis());
        } else if(!club.isSynced()) {
            Long monolithClubId = club.getMonolithId();
            Club monolithClub = monolithReadWriteApiAdapter.getClubById(monolithClubId);
            applyChanges(monolithClub, club);
        }
        club.setSynced(true);
        clubRepository.save(club);
    }

    public void syncPlayerEntityIfNecessary(Long playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if(player != null && player.isSynced()) return;
        if(player == null) {
            Player monolithPlayer = monolithReadWriteApiAdapter.getPlayerById(playerId);
            player = new Player(monolithPlayer);
            player.setId(System.currentTimeMillis());
        } else if(!player.isSynced()) {
            Long monolithPlayerId = player.getMonolithId();
            Player monolithPlayer = monolithReadWriteApiAdapter.getPlayerById(monolithPlayerId);
            applyChanges(monolithPlayer, player);
        }
        player.setSynced(true);
        playerRepository.save(player);
    }


    private void applyChanges(Club monolithClub, Club serviceClub) {
        serviceClub.setName(monolithClub.getName());
        serviceClub.setCountry(monolithClub.getCountry());
        serviceClub.setPresident(monolithClub.getPresident());
        serviceClub.setCreated(monolithClub.getCreated());
        serviceClub.setModified(monolithClub.getModified());
        serviceClub.setMonolithId(monolithClub.getId());
    }

    private void applyChanges(Player monolithPlayer, Player servicePlayer) {
        servicePlayer.setName(monolithPlayer.getName());
        servicePlayer.setCountry(monolithPlayer.getCountry());
        servicePlayer.setRating(monolithPlayer.getRating());
        servicePlayer.setCreated(monolithPlayer.getCreated());
        servicePlayer.setModified(monolithPlayer.getModified());
        if(monolithPlayer.getClubId() != null) {
            Long serviceClubId = resolveServiceClubId(monolithPlayer.getClubId());
            servicePlayer.setClubId(serviceClubId);
        }
        servicePlayer.setMonolithId(monolithPlayer.getId());
    }

    private Long resolveServiceClubId(Long clubId) {
        Optional<Club> club = clubRepository.findByMonolithId(clubId);
        return club.map(Club::getId).orElse(clubId);
    }

    private Long resolveServicePlayerId(Long playerId) {
        Optional<Player> player = playerRepository.findByMonolithId(playerId);
        return player.map(Player::getId).orElse(playerId);
    }
}
