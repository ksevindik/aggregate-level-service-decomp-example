package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.IdMapping;
import com.example.clubservice.model.Player;
import com.example.clubservice.repository.IdMappingRepository;
import com.example.clubservice.service.ClubService;
import com.example.clubservice.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class ReadWriteApiDispatcher {

    @Autowired
    private MonolithReadWriteApiAdapter monolithReadWriteApiAdapter;

    @Autowired
    private ClubService clubService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private EntityPersister entityPersister;

    @Autowired
    private IdMappingRepository idMappingRepository;

    @Autowired
    private OperationModeManager operationModeManager;

    //club related operations

    public List<Club> getAllClubs() {
        return executeCommand(
                () -> clubService.getAllClubs(),
                () -> clubService.getAllClubs(),
                () -> monolithReadWriteApiAdapter.getAllClubs());
    }

    public List<Club> getClubsByCountry(String country) {
        return executeCommand(
                () -> clubService.getClubsByCountry(country),
                () -> clubService.getClubsByCountry(country),
                () -> monolithReadWriteApiAdapter.getClubsByCountry(country));
    }

    public Optional<Club> getClubById(Long id) {
        return executeCommand(
                () -> clubService.getClubById(id),
                () -> clubService.getClubById(id),
                () -> Optional.ofNullable(monolithReadWriteApiAdapter.getClubById(id)));
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        return executeCommand(
                () -> clubService.getClubsByNamePattern(namePattern),
                () -> clubService.getClubsByNamePattern(namePattern),
                () -> monolithReadWriteApiAdapter.getClubsByNamePattern(namePattern));
    }

    public Club createClub(Club club) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.createClub(club),
                () -> clubService.createClub(club),
                () -> {
                    Club monolithSavedClub = monolithReadWriteApiAdapter.createClub(club);
                    Club serviceSavedClub = clubService.createClub(club);
                    idMappingRepository.save(new IdMapping(
                            serviceSavedClub.getId(), monolithSavedClub.getId(), "Club"));
                    return monolithSavedClub;
                });
    }

    public Club updatePresident(Long clubId, String president) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.updatePresident(clubId, president),
                () -> {
                    entityPersister.syncClubEntityIfNecessary(clubId);
                    return clubService.updatePresident(clubId, president);
                },
                () -> {
                    Club monolithUpdatedClub = monolithReadWriteApiAdapter.updatePresident(clubId, president);
                    Long serviceCLubId = idMappingRepository.findByMonolithIdAndTypeName(clubId, "Club").getServiceId();
                    clubService.updatePresident(serviceCLubId, president);
                    return monolithUpdatedClub;
                });
    }

    //player related operations

    public List<Player> getAllPlayers() {
        return executeCommand(
                () -> playerService.getAllPlayers(),
                () -> playerService.getAllPlayers(),
                () -> monolithReadWriteApiAdapter.getAllPlayers());
    }

    public List<Player> getPlayersByClubName(String clubName) {
        return executeCommand(
                () -> playerService.getPlayersByClubName(clubName),
                () -> playerService.getPlayersByClubName(clubName),
                () -> monolithReadWriteApiAdapter.getPlayersByClubName(clubName));
    }

    public List<Player> getPlayersByCountry(String country) {
        return executeCommand(
                () -> playerService.getPlayersByCountry(country),
                () -> playerService.getPlayersByCountry(country),
                () -> monolithReadWriteApiAdapter.getPlayersByCountry(country));
    }

    public Optional<Player> getPlayerById(Long id) {
        return executeCommand(
                () -> playerService.getPlayerById(id),
                () -> playerService.getPlayerById(id),
                () -> Optional.ofNullable(monolithReadWriteApiAdapter.getPlayerById(id)));
    }

    public List<Player> getPlayersByNamePattern(String name) {
        return executeCommand(
                () -> playerService.getPlayersByNamePattern(name),
                () -> playerService.getPlayersByNamePattern(name),
                () -> monolithReadWriteApiAdapter.getPlayersByNamePattern(name));
    }

    public Player createPlayer(Player player) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.createPlayer(player),
                () -> {
                    player.setSynced(true);
                    return playerService.createPlayer(player);
                },
                () -> {
                    Player monolithSavedPlayer = monolithReadWriteApiAdapter.createPlayer(player);
                    IdMapping idMapping = idMappingRepository.findByMonolithIdAndTypeName(player.getClubId(), "Club");
                    player.setClubId(idMapping.getServiceId());
                    Player serviceSavedPlayer = playerService.createPlayer(player);
                    idMappingRepository.save(new IdMapping(
                            serviceSavedPlayer.getId(), monolithSavedPlayer.getId(), "Player"));
                    return monolithSavedPlayer;
                });
    }

    public Player updateRating(Long playerId, Integer rating) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.updatePlayerRating(playerId, rating),
                () -> {
                    entityPersister.syncPlayerEntityIfNecessary(playerId);
                    return playerService.updatePlayerRating(playerId, rating);
                    },
                () -> {
                    Player monolithUpdatedPlayer = monolithReadWriteApiAdapter.updatePlayerRating(playerId, rating);
                    Long servicePlayerId = idMappingRepository.findByMonolithIdAndTypeName(playerId, "Player").getServiceId();
                    playerService.updatePlayerRating(servicePlayerId, rating);
                    return monolithUpdatedPlayer;
                });
    }

    public Player transferPlayer(Long playerId, Long clubId) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.transferPlayer(playerId, clubId),
                () -> {entityPersister.syncPlayerEntityIfNecessary(playerId);
                        return playerService.transferPlayer(playerId, clubId);
                },
                () -> {
                    Player monolithUpdatedPlayer = monolithReadWriteApiAdapter.transferPlayer(playerId, clubId);
                    Long servicePlayerId = idMappingRepository.findByMonolithIdAndTypeName(playerId, "Player").getServiceId();
                    Long serviceClubId = idMappingRepository.findByMonolithIdAndTypeName(clubId, "Club").getServiceId();
                    playerService.transferPlayer(servicePlayerId, serviceClubId);
                    return monolithUpdatedPlayer;
                });
    }

    private <R> R executeCommand(Supplier<R> readOnlyCommand,
                                 Supplier<R> readWriteCommand,
                                 Supplier<R> dryRunCommand) {
        switch (operationModeManager.getOperationMode()) {
            case READ_ONLY:
                return readOnlyCommand.get();
            case READ_WRITE:
                return readWriteCommand.get();
            case DRY_RUN:
                return dryRunCommand.get();
            default:
                throw new IllegalStateException(
                        "Unknown operation mode: " + operationModeManager.getOperationMode());
        }
    }
}
