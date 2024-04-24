package com.example.clubservice.migration;

import com.example.clubservice.model.Club;
import com.example.clubservice.model.Player;
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
    private OperationModeManager operationModeManager;

    //club related operations

    public List<Club> getAllClubs() {
        return executeCommand(
                () -> clubService.getAllClubs(),
                () -> clubService.getAllClubs(),
                () -> monolithReadWriteApiAdapter.getAllClubs(),
                () -> {});
    }

    public List<Club> getClubsByCountry(String country) {
        return executeCommand(
                () -> clubService.getClubsByCountry(country),
                () -> clubService.getClubsByCountry(country),
                () -> monolithReadWriteApiAdapter.getClubsByCountry(country),
                () -> {});
    }

    public Optional<Club> getClubById(Long id) {
        return executeCommand(
                () -> clubService.getClubById(id),
                () -> clubService.getClubById(id),
                () -> Optional.ofNullable(monolithReadWriteApiAdapter.getClubById(id)),
                () -> {});
    }

    public List<Club> getClubsByNamePattern(String namePattern) {
        return executeCommand(
                () -> clubService.getClubsByNamePattern(namePattern),
                () -> clubService.getClubsByNamePattern(namePattern),
                () -> monolithReadWriteApiAdapter.getClubsByNamePattern(namePattern),
                () -> {});
    }

    public Club createClub(Club club) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.createClub(club),
                () -> clubService.createClub(club),
                () -> monolithReadWriteApiAdapter.createClub(club),
                () -> {});
    }

    public Club updatePresident(Long clubId, String president) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.updatePresident(clubId, president),
                () -> clubService.updatePresident(clubId, president),
                () -> monolithReadWriteApiAdapter.updatePresident(clubId, president),
                () -> entityPersister.syncClubEntityIfNecessary(clubId));
    }

    //player related operations

    public List<Player> getAllPlayers() {
        return executeCommand(
                () -> playerService.getAllPlayers(),
                () -> playerService.getAllPlayers(),
                () -> monolithReadWriteApiAdapter.getAllPlayers(),
                () -> {});
    }

    public List<Player> getPlayersByClubName(String clubName) {
        return executeCommand(
                () -> playerService.getPlayersByClubName(clubName),
                () -> playerService.getPlayersByClubName(clubName),
                () -> monolithReadWriteApiAdapter.getPlayersByClubName(clubName),
                () -> {});
    }

    public List<Player> getPlayersByCountry(String country) {
        return executeCommand(
                () -> playerService.getPlayersByCountry(country),
                () -> playerService.getPlayersByCountry(country),
                () -> monolithReadWriteApiAdapter.getPlayersByCountry(country),
                () -> {});
    }

    public Optional<Player> getPlayerById(Long id) {
        return executeCommand(
                () -> playerService.getPlayerById(id),
                () -> playerService.getPlayerById(id),
                () -> Optional.ofNullable(monolithReadWriteApiAdapter.getPlayerById(id)),
                () -> {});
    }

    public List<Player> getPlayersByNamePattern(String name) {
        return executeCommand(
                () -> playerService.getPlayersByNamePattern(name),
                () -> playerService.getPlayersByNamePattern(name),
                () -> monolithReadWriteApiAdapter.getPlayersByNamePattern(name),
                () -> {});
    }

    public Player createPlayer(Player player) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.createPlayer(player),
                () -> playerService.createPlayer(player),
                () -> monolithReadWriteApiAdapter.createPlayer(player),
                () -> {});
    }

    public Player updateRating(Long playerId, Integer rating) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.updatePlayerRating(playerId, rating),
                () -> playerService.updatePlayerRating(playerId, rating),
                () -> monolithReadWriteApiAdapter.updatePlayerRating(playerId, rating),
                () -> entityPersister.syncPlayerEntityIfNecessary(playerId));
    }

    public Player transferPlayer(Long playerId, Long clubId) {
        return executeCommand(
                () -> monolithReadWriteApiAdapter.transferPlayer(playerId, clubId),
                () -> playerService.transferPlayer(playerId, clubId),
                () -> monolithReadWriteApiAdapter.transferPlayer(playerId, clubId),
                () -> entityPersister.syncPlayerEntityIfNecessary(playerId));
    }

    private <R> R executeCommand(Supplier<R> serviceReadOnlyCommand,
                                 Supplier<R> serviceReadWriteCommand,
                                 Supplier<R> monolithReadWriteCommand,
                                 Runnable entityStateSyncCommand) {
        switch (operationModeManager.getOperationMode()) {
            case READ_ONLY:
                /*
                read-only means that only read-only operations should be served from the service,
                all write operations should be forwarded to the monolith
                 */
                return serviceReadOnlyCommand.get();
            case READ_WRITE:
                /*
                read-write means that all read and write operations should be served from the service,
                before executing the write operation, the entity state should be synced from the monolith if necessary.
                 */
                entityStateSyncCommand.run();
                return serviceReadWriteCommand.get();
            case DRY_RUN:
                /*
                dry-run means that all read and write operations should be forwarded to the monolith,
                meanwhile write operations should be executed on the service as well without causing any
                external side effects on the other parties.
                 */
                R result = monolithReadWriteCommand.get();
                try {
                    serviceReadWriteCommand.get();
                } catch (Exception e) {
                    //deliberately ignore the exception
                }
                return result;
            default:
                throw new IllegalStateException(
                        "Unknown operation mode: " + operationModeManager.getOperationMode());
        }
    }
}
