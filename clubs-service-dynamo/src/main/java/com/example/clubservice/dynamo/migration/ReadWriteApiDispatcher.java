package com.example.clubservice.dynamo.migration;
import com.example.clubservice.dynamo.model.Club;
import com.example.clubservice.dynamo.model.Player;
import com.example.clubservice.dynamo.repository.ClubRepository;
import com.example.clubservice.dynamo.repository.PlayerRepository;
import com.example.clubservice.dynamo.service.ClubService;
import com.example.clubservice.dynamo.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
/*
the job of read write api dispatcher is to dispatch the read and write operations to the service layer or the monolith
according to the current operation mode, while doing necessary id mapping between the monolith and the service, and data syncing etc.
 */
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
    private OperationModeManager operationModeManager;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PlayerRepository playerRepository;

    //club related operations

    public List<Club> getAllClubs() {
        return executeCommand(
                () -> clubService.getAllClubs().stream().map(this::convertToMonolithIds).toList(),
                () -> clubService.getAllClubs(),
                () -> monolithReadWriteApiAdapter.getAllClubs());
    }

    public List<Club> getClubsByCountry(String country) {
        return executeCommand(
                () -> clubService.getClubsByCountry(country).stream().map(this::convertToMonolithIds).toList(),
                () -> clubService.getClubsByCountry(country),
                () -> monolithReadWriteApiAdapter.getClubsByCountry(country));
    }

    public Optional<Club> getClubById(Long id) {
        return executeCommand(
                () -> {
                    Long clubId = resolveServiceClubId(id);
                    Club club = clubService.getClubById(clubId).orElse(null);
                    return Optional.ofNullable(convertToMonolithIds(club));
                },
                () -> clubService.getClubById(id),
                () -> Optional.ofNullable(monolithReadWriteApiAdapter.getClubById(id)));
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        return executeCommand(
                () -> clubService.getClubsByNamePattern(namePattern).stream().map(this::convertToMonolithIds).toList(),
                () -> clubService.getClubsByNamePattern(namePattern),
                () -> monolithReadWriteApiAdapter.getClubsByNamePattern(namePattern));
    }

    public Club createClub(Club club) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.createClub(club),
                () -> clubService.createClub(club),
                () -> {
                    Club monolithSavedClub = monolithReadWriteApiAdapter.createClub(club);
                    club.setMonolithId(monolithSavedClub.getId());
                    clubService.createClub(club);
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
                    Long serviceCLubId = resolveServiceClubId(monolithUpdatedClub.getId());
                    clubService.updatePresident(serviceCLubId, president);
                    return monolithUpdatedClub;
                });
    }

    //player related operations

    public List<Player> getAllPlayers() {
        return executeCommand(
                () -> playerService.getAllPlayers().stream().map(this::convertToMonolithIds).toList(),
                () -> playerService.getAllPlayers(),
                () -> monolithReadWriteApiAdapter.getAllPlayers());
    }

    public List<Player> getPlayersByClubName(String clubName) {
        return executeCommand(
                () -> playerService.getPlayersByClubName(clubName).stream().map(this::convertToMonolithIds).toList(),
                () -> playerService.getPlayersByClubName(clubName),
                () -> monolithReadWriteApiAdapter.getPlayersByClubName(clubName));
    }

    public List<Player> getPlayersByCountry(String country) {
        return executeCommand(
                () -> playerService.getPlayersByCountry(country).stream().map(this::convertToMonolithIds).toList(),
                () -> playerService.getPlayersByCountry(country),
                () -> monolithReadWriteApiAdapter.getPlayersByCountry(country));
    }

    public Optional<Player> getPlayerById(Long id) {
        return executeCommand(
                () -> {
                    Long serviceId = resolveServicePlayerId(id);
                    Player player = playerService.getPlayerById(serviceId).orElse(null);
                    return Optional.ofNullable(convertToMonolithIds(player));
                },
                () -> playerService.getPlayerById(id),
                () -> Optional.ofNullable(monolithReadWriteApiAdapter.getPlayerById(id)));
    }

    public List<Player> getPlayersByNamePattern(String name) {
        return executeCommand(
                () -> playerService.getPlayersByNamePattern(name).stream().map(this::convertToMonolithIds).toList(),
                () -> playerService.getPlayersByNamePattern(name),
                () -> monolithReadWriteApiAdapter.getPlayersByNamePattern(name));
    }

    public Player createPlayer(Player player) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.createPlayer(player),
                () -> playerService.createPlayer(player),
                () -> {
                    Player monolithSavedPlayer = monolithReadWriteApiAdapter.createPlayer(player);
                    if(monolithSavedPlayer.getClubId() != null) {
                        Long serviceClubId = resolveServiceClubId(monolithSavedPlayer.getClubId());
                        player.setClubId(serviceClubId);
                    }
                    player.setMonolithId(monolithSavedPlayer.getId());
                    playerService.createPlayer(player);
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
                    Long servicePlayerId = resolveServicePlayerId(monolithUpdatedPlayer.getId());
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
                    Long servicePlayerId = resolveServicePlayerId(playerId);
                    Long serviceClubId = resolveServiceClubId(clubId);
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

    private Long resolveServiceClubId(Long clubId) {
        Optional<Club> club = clubRepository.findByMonolithId(clubId);
        return club.isEmpty() ? clubId: club.get().getId();
    }

    private Long resolveServicePlayerId(Long playerId) {
        Optional<Player> player = playerRepository.findByMonolithId(playerId);
        return player.isEmpty() ? playerId: player.get().getId();
    }

    private Club convertToMonolithIds(Club club) {
        if(club == null) return null;
        Club monolithClub = new Club(club);
        monolithClub.setId(club.getMonolithId());
        monolithClub.setMonolithId(null);
        return monolithClub;
    }

    private Player convertToMonolithIds(Player player) {
        if(player == null) return null;
        Player monolithPlayer = new Player(player);
        monolithPlayer.setId(player.getMonolithId());
        monolithPlayer.setMonolithId(null);
        if(player.getClubId() != null) {
            monolithPlayer.setClubId(clubRepository.findById(player.getClubId()).map(Club::getMonolithId).orElse(null));
        }
        return monolithPlayer;
    }
}
