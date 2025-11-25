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
    public List<CasualGroup> startRound(Long tournamentId) {
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
    public List<CasualGroup> reshuffleReady(Long tournamentId, Long adminId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!tournament.getOwner().getId().equals(adminId)) {
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
     */
    @Transactional
    public void markUserReady(Long tournamentId, Long userId, Long requesterId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isAdmin = tournament.getOwner().getId().equals(requesterId);
        boolean isSelf = userId.equals(requesterId);

        if (!isAdmin && !isSelf) {
            throw new IllegalStateException("Only the user or admin can mark this user as ready");
        }

        if (!tournament.getUsers().stream().anyMatch(u -> u.getId().equals(userId))) {
            throw new IllegalStateException("User is not registered in this tournament");
        }

        if (tournament.getUsersReady().stream().noneMatch(u -> u.getId().equals(userId))) {
            tournament.getUsersReady().add(user);
            tournamentCasualRepository.save(tournament);
        }
    }

    /**
     * Mark a user as not ready (by admin only).
     */
    @Transactional
    public void markUserNotReady(Long tournamentId, Long userId, Long adminId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!tournament.getOwner().getId().equals(adminId)) {
            throw new IllegalStateException("Only the tournament owner can mark users as not ready");
        }

        tournament.getUsersReady().removeIf(u -> u.getId().equals(userId));
        tournamentCasualRepository.save(tournament);
    }

    /**
     * Get all ready users in a tournament.
     */
    public List<User> getReadyUsers(Long tournamentId) {
        TournamentCasual tournament = tournamentCasualRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        return tournament.getUsersReady();
    }

    /**
     * Get all groups in a tournament.
     */
    public List<CasualGroup> getGroups(Long tournamentId) {
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
