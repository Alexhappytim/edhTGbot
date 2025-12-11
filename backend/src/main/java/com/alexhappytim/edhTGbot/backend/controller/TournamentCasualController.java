package com.alexhappytim.edhTGbot.backend.controller;

import com.alexhappytim.edhTGbot.backend.model.*;
import com.alexhappytim.edhTGbot.backend.repository.TournamentCasualRepository;
import com.alexhappytim.edhTGbot.backend.repository.TournamentRepository;
import com.alexhappytim.edhTGbot.backend.repository.UserRepository;
import com.alexhappytim.edhTGbot.backend.service.CasualTournamentService;
import com.alexhappytim.mtg.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tournamentsCasual")
@RequiredArgsConstructor
public class TournamentCasualController {
    private final TournamentCasualRepository tournamentCasualRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final CasualTournamentService casualTournamentService;

    @PostMapping
    public ResponseEntity<TournamentCasualDTO> createTournament(@RequestBody CreateTournamentRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tournament name is required");
        }
        User owner = userRepository.findByTelegramId(request.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("User not found. Please register first using /register"));

        // Create parent Tournament entity with type
        Tournament parent = Tournament.builder()
                .type(TournamentType.CASUAL)
                .build();
        parent = tournamentRepository.save(parent);

        TournamentCasual tournament = TournamentCasual.builder()
                .id(parent.getId())
                .tournament(parent)
                .name(request.getName())
                .owner(owner)
                .build();
        tournament = tournamentCasualRepository.save(tournament);
        return ResponseEntity.ok(toDTO(tournament));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<UserDTO> addUser(@PathVariable String id, @RequestBody JoinTournamentRequest request) {
        Optional<TournamentCasual> tournamentOpt = tournamentCasualRepository.findById(id);
        if (tournamentOpt.isEmpty()) {
            throw new IllegalArgumentException("Tournament not found");
        }
        final User tempUser;
        Optional<User> userOpt = userRepository.findByTelegramId(request.getUserId());
        if (request.getIsTemporary()) {
            if (!Objects.equals(tournamentOpt.get().getOwner().getTelegramId(), request.getUserId())) {
                throw new IllegalArgumentException("It is not your tournament");
            }
            tempUser = userRepository.save(User.builder()
                    .userTag("")
                    .displayName(request.getParticipantName())
                    .telegramId((long) -1)
                    .chatId((long) -1)
                    .build());
        } else {
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            } else {
                tempUser = userOpt.get();
            }
        }

        TournamentCasual tournament = tournamentOpt.get();

        if (tournament.getUsers().stream().anyMatch(p -> p.getId().equals(tempUser.getId())) && !request.getIsTemporary()) {
            throw new IllegalStateException("User already registered in this tournament");
        }

        tournament.getUsers().add(tempUser);
        tournamentCasualRepository.save(tournament);

        return ResponseEntity.ok(toUserDTO(tempUser));
    }

    @PostMapping("/{id}/start-round")
    public ResponseEntity<List<CasualGroupDTO>> startNextRound(@PathVariable String id) {
        List<CasualGroup> groups = casualTournamentService.startRound(id);
        return ResponseEntity.ok(groups.stream().map(this::toGroupDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/reshuffle")
    public ResponseEntity<List<CasualGroupDTO>> reshuffleReady(@PathVariable String id, @RequestParam Long adminId) {
        List<CasualGroup> groups = casualTournamentService.reshuffleReady(id, adminId);
        return ResponseEntity.ok(groups.stream().map(this::toGroupDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<Void> markUserReady(@PathVariable String id, @RequestParam Integer playerPosition, @RequestParam Long adminId) {
        casualTournamentService.markUserReady(id, playerPosition, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/self-ready")
    public ResponseEntity<Void> markSelfReady(@PathVariable String id, @RequestParam Long userId) {
        casualTournamentService.markSelfReady(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/not-ready")
    public ResponseEntity<Void> markUserNotReady(@PathVariable String id, @RequestParam Integer playerPosition, @RequestParam Long adminId) {
        casualTournamentService.markUserNotReady(id, playerPosition, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/ready-all")
    public ResponseEntity<Void> markAllReady(@PathVariable String id, @RequestParam Long adminId) {
        casualTournamentService.markAllReady(id, adminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/ready-group")
    public ResponseEntity<Void> markGroupReady(@PathVariable String id, @RequestParam Integer groupNumber, @RequestParam Long adminId) {
        casualTournamentService.markGroupReady(id, groupNumber, adminId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/ready-users")
    public ResponseEntity<List<UserDTO>> getReadyUsers(@PathVariable String id) {
        List<User> readyUsers = casualTournamentService.getReadyUsers(id);
        return ResponseEntity.ok(readyUsers.stream().map(this::toUserDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/groups")
    public ResponseEntity<List<CasualGroupDTO>> getGroups(@PathVariable String id) {
        List<CasualGroup> groups = casualTournamentService.getGroups(id);
        return ResponseEntity.ok(groups.stream().map(this::toGroupDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentCasualDTO> getTournament(@PathVariable String id) {
        TournamentCasual tournament = tournamentCasualRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        return ResponseEntity.ok(toDTO(tournament));
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setDisplayName(user.getDisplayName());
        dto.setTelegramId(user.getTelegramId());
        dto.setChatId(user.getChatId());
        dto.setUserTag(user.getUserTag());
        return dto;
    }

    private CasualGroupDTO toGroupDTO(CasualGroup group) {
        CasualGroupDTO dto = new CasualGroupDTO();
        dto.setId(group.getId());
        dto.setGroupNumber(group.getGroupNumber());
        dto.setPlayers(group.getPlayers().stream().map(this::toUserDTO).collect(Collectors.toList()));
        return dto;
    }

    private TournamentCasualDTO toDTO(TournamentCasual tournament) {
        TournamentCasualDTO dto = new TournamentCasualDTO();
        dto.setId(tournament.getId());
        dto.setName(tournament.getName());
        dto.setOwner(toUserDTO(tournament.getOwner()));
        dto.setUsers(tournament.getUsers().stream().map(this::toUserDTO).collect(Collectors.toList()));
        dto.setReadyUsers(tournament.getUsersReady().stream().map(this::toUserDTO).collect(Collectors.toList()));
        dto.setGroups(tournament.getGroups().stream().map(this::toGroupDTO).collect(Collectors.toList()));
        return dto;
    }
}
