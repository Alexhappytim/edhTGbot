package com.alexhappytim.edhTGbot.backend.service;

import com.alexhappytim.edhTGbot.backend.model.CasualGroup;
import com.alexhappytim.edhTGbot.backend.model.TournamentCasual;
import com.alexhappytim.edhTGbot.backend.model.User;
import com.alexhappytim.edhTGbot.backend.repository.CasualGroupRepository;
import com.alexhappytim.edhTGbot.backend.repository.TournamentCasualRepository;
import com.alexhappytim.edhTGbot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CasualTournamentService {
    private final TournamentCasualRepository tournamentCasualRepository;
    private final CasualGroupRepository casualGroupRepository;
    private final UserRepository userRepository;

    /**
     * Start a round: mark all users as ready, shuffle into groups of 4, then mark all as not ready.
     */
    @Transactional
    public List<CasualGroup> startRound(String tournamentId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (tournament.getUsers().size() < 4) {
            throw new IllegalStateException("Not enough users to start a round (minimum 4)");
        }

        // Clear existing groups
        casualGroupRepository.deleteByTournamentId(tournamentId);
        tournament.getGroups().clear();

        // Mark all users as ready initially
        List<User> allUsers = new ArrayList<>(tournament.getUsers());
        tournament.getUsersReady().clear();
        tournament.getUsersReady().addAll(allUsers);

        // Shuffle and create groups of 4
        List<CasualGroup> groups = shuffleIntoGroups(tournament, allUsers);

        // Mark all users as not ready after grouping
        tournament.getUsersReady().clear();

        tournamentCasualRepository.save(tournament);

        return groups;
    }

    /**
     * Reshuffle only ready users into new groups.
     */
    @Transactional
    public List<CasualGroup> reshuffleReady(String tournamentId, Long adminTelegramId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!tournament.getOwner().getTelegramId().equals(adminTelegramId)) {
            throw new IllegalStateException("Only the tournament owner can reshuffle");
        }

        List<User> readyUsers = new ArrayList<>(tournament.getUsersReady());
        if (readyUsers.size() < 4) {
            throw new IllegalStateException("Not enough ready users to reshuffle (minimum 4)");
        }

        // Remove old groups that contain only ready users
        List<CasualGroup> existingGroups = new ArrayList<>(tournament.getGroups());
        for (CasualGroup group : existingGroups) {
            boolean allReady = group.getPlayers().stream()
                    .allMatch(p -> readyUsers.stream().anyMatch(r -> r.getId().equals(p.getId())));
            if (allReady) {
                tournament.getGroups().remove(group);
                casualGroupRepository.delete(group);
            }
        }

        // Shuffle ready users into new groups
        List<CasualGroup> newGroups = shuffleIntoGroups(tournament, readyUsers);

        // Mark reshuffled users as not ready
        tournament.getUsersReady().clear();

        tournamentCasualRepository.save(tournament);
        return newGroups;
    }

    /**
     * Mark a user as ready (by user themselves or by admin).
     * @param playerPosition 1-based position of player in tournament (for admin use)
     * @param requesterTelegramId Telegram ID of the requester
     */
    @Transactional
    public void markUserReady(String tournamentId, Integer playerPosition, Long requesterTelegramId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        boolean isAdmin = tournament.getOwner().getTelegramId().equals(requesterTelegramId);

        if (!isAdmin) {
            throw new IllegalStateException("Only the tournament owner can mark users as ready by position");
        }

        List<User> users = new ArrayList<>(tournament.getUsers());
        if (playerPosition < 1 || playerPosition > users.size()) {
            throw new IllegalArgumentException("Invalid player position. Must be between 1 and " + users.size());
        }

        User user = users.get(playerPosition - 1);

        if (tournament.getUsersReady().stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            tournament.getUsersReady().add(user);
            tournamentCasualRepository.save(tournament);
        }
    }

    /**
     * Mark self as ready (by user themselves).
     */
    @Transactional
    public void markSelfReady(String tournamentId, Long userTelegramId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        User user = userRepository.findByTelegramId(userTelegramId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!tournament.getUsers().stream().anyMatch(u -> u.getTelegramId().equals(userTelegramId))) {
            throw new IllegalStateException("User is not registered in this tournament");
        }

        if (tournament.getUsersReady().stream().noneMatch(u -> u.getTelegramId().equals(userTelegramId))) {
            tournament.getUsersReady().add(user);
            tournamentCasualRepository.save(tournament);
        }
    }

    /**
     * Mark a user as not ready (by admin only).
     * @param playerPosition 1-based position of player in tournament
     */
    @Transactional
    public void markUserNotReady(String tournamentId, Integer playerPosition, Long adminTelegramId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!tournament.getOwner().getTelegramId().equals(adminTelegramId)) {
            throw new IllegalStateException("Only the tournament owner can mark users as not ready");
        }

        List<User> users = new ArrayList<>(tournament.getUsers());
        if (playerPosition < 1 || playerPosition > users.size()) {
            throw new IllegalArgumentException("Invalid player position. Must be between 1 and " + users.size());
        }

        User user = users.get(playerPosition - 1);
        tournament.getUsersReady().removeIf(u -> u.getId().equals(user.getId()));
        tournamentCasualRepository.save(tournament);
    }

    /**
     * Mark all users as ready for shuffle.
     */
    @Transactional
    public void markAllReady(String tournamentId, Long adminTelegramId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!tournament.getOwner().getTelegramId().equals(adminTelegramId)) {
            throw new IllegalStateException("Only the tournament owner can mark all users as ready");
        }

        tournament.getUsersReady().clear();
        tournament.getUsersReady().addAll(tournament.getUsers());
        tournamentCasualRepository.save(tournament);
    }

    /**
     * Mark all players from a specific group as ready for shuffle.
     * @param groupNumber The group number (1-based)
     */
    @Transactional
    public void markGroupReady(String tournamentId, Integer groupNumber, Long adminTelegramId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!tournament.getOwner().getTelegramId().equals(adminTelegramId)) {
            throw new IllegalStateException("Only the tournament owner can mark group as ready");
        }

        CasualGroup group = tournament.getGroups().stream()
                .filter(g -> g.getGroupNumber() == groupNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Group " + groupNumber + " not found"));

        // Add all players from this group to ready list (avoiding duplicates)
        for (User player : group.getPlayers()) {
            if (tournament.getUsersReady().stream().noneMatch(u -> u.getId().equals(player.getId()))) {
                tournament.getUsersReady().add(player);
            }
        }
        tournamentCasualRepository.save(tournament);
    }

    /**
     * Get all ready users in a tournament.
     */
    public List<User> getReadyUsers(String tournamentId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        return tournament.getUsersReady();
    }

    /**
     * Get all groups in a tournament.
     */
    public List<CasualGroup> getGroups(String tournamentId) {
        return casualGroupRepository.findByTournamentId(tournamentId);
    }

    /**
     * Shuffle users into groups of 4 and save.
     */
    private List<CasualGroup> shuffleIntoGroups(TournamentCasual tournament, List<User> users) {
        Collections.shuffle(users);
        List<CasualGroup> groups = new ArrayList<>();
        int groupNumber = tournament.getGroups().size() + 1;

        for (int i = 0; i < users.size(); i += 4) {
            List<User> groupPlayers = new ArrayList<>();
            for (int j = i; j < Math.min(i + 4, users.size()); j++) {
                groupPlayers.add(users.get(j));
            }
            // Only create groups of at least 2 players
            if (groupPlayers.size() >= 2) {
                CasualGroup group = CasualGroup.builder()
                        .tournament(tournament)
                        .players(groupPlayers)
                        .groupNumber(groupNumber++)
                        .build();
                group = casualGroupRepository.save(group);
                tournament.getGroups().add(group);
                groups.add(group);
            }
        }
        return groups;
    }
}
