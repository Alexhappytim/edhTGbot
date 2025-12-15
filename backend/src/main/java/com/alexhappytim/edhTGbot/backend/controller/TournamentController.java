package com.alexhappytim.edhTGbot.backend.controller;

import com.alexhappytim.edhTGbot.backend.model.*;
import com.alexhappytim.edhTGbot.backend.repository.*;
import com.alexhappytim.edhTGbot.backend.service.SwissTournamentService;
import com.alexhappytim.edhTGbot.backend.service.CasualTournamentService;
import com.alexhappytim.mtg.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {
    private final TournamentRepository tournamentRepository;
    private final SwissTournamentRepository swissTournamentRepository;
    private final TournamentCasualRepository casualRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final SwissTournamentService swissTournamentService;
    private final CasualTournamentService casualTournamentService;
    //TODO FUCKING REWRITE THIS SHIT
    @Transactional
    @PostMapping
    public ResponseEntity<TournamentDTO> createTournament(@RequestBody CreateTournamentRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tournament name is required");
        }
        if (request.getMaxPlayers() < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 players");
        }
        User owner = userRepository.findByTelegramId(request.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("User not found. Please register first using /register"));
        // Create parent Tournament entity with type
        Tournament parent = Tournament.builder()
                .type(TournamentType.SWISS)
                .build();
        parent = tournamentRepository.save(parent);

        // Create SwissTournament with reference to parent
        SwissTournament tournament = SwissTournament.builder()
                .tournament(parent)
                .name(request.getName())
                .maxPlayers(request.getMaxPlayers())
                .status(TournamentStatus.REGISTRATION)
                .owner(owner)
                .build();
        tournament = swissTournamentRepository.save(tournament);
        TournamentDTO dto = new TournamentDTO();
        dto.setId(tournament.getId());
        dto.setName(tournament.getName());
        dto.setMaxPlayers(tournament.getMaxPlayers());
        dto.setStatus(tournament.getStatus().name());
        dto.setParticipants(new ArrayList<>());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<JoinTournamentResponse> joinTournament(@PathVariable String id, @RequestBody JoinTournamentRequest request) {
        // Look up the parent Tournament to determine type
        Tournament parent = tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        JoinTournamentResponse response = new JoinTournamentResponse();
        response.setTournamentId(id);
        response.setTournamentType(parent.getType().name());

        if (parent.getType() == TournamentType.SWISS) {
            // Swiss tournament join logic with temporary user support
            SwissTournament tournament = swissTournamentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Swiss tournament details not found"));

            if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
                throw new IllegalStateException("Cannot join: Tournament is not in registration phase");
            }

            if (tournament.getParticipants().size() >= tournament.getMaxPlayers()) {
                throw new IllegalStateException("Tournament is full");
            }

            final User participantUser;
            
            if (request.getIsTemporary()) {
                // Verify the requester is the tournament owner
                User owner = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
                        
                if (!tournament.getOwner().getId().equals(owner.getId())) {
                    throw new IllegalArgumentException("Only tournament owner can add temporary participants");
                }
                
                // Create temporary user
                participantUser = userRepository.save(User.builder()
                        .userTag("")
                        .displayName(request.getParticipantName())
                        .telegramId(-1L)
                        .chatId(-1L)
                        .build());
            } else {
                // Regular user join
                participantUser = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                        
                if (tournament.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(participantUser.getId()))) {
                    // User already registered, return success with flag
                    response.setJoined(false);
                    response.setMessage("User already registered in this tournament");
                    return ResponseEntity.ok(response);
                }
            }

            Participant participant = Participant.builder()
                    .tournament(tournament)
                    .user(participantUser)
                    .points(0)
                    .tieBreaker(0)
                    .build();
            participant = participantRepository.save(participant);
            tournament.getParticipants().add(participant);
            swissTournamentRepository.save(tournament);

            response.setJoined(true);
            response.setMessage("Successfully joined Swiss tournament");

        } else if (parent.getType() == TournamentType.CASUAL) {
            // Casual tournament join logic with temporary user support
            TournamentCasual tournament = casualRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Casual tournament details not found"));

            final User participantUser;
            
            if (request.getIsTemporary()) {
                // Verify the requester is the tournament owner
                User owner = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
                        
                if (!tournament.getOwner().getId().equals(owner.getId())) {
                    throw new IllegalArgumentException("Only tournament owner can add temporary participants");
                }
                
                // Create temporary user
                participantUser = userRepository.save(User.builder()
                        .userTag("")
                        .displayName(request.getParticipantName())
                        .telegramId(-1L)
                        .chatId(-1L)
                        .build());
            } else {
                // Regular user join
                participantUser = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                        
                if (tournament.getUsers().contains(participantUser)) {
                    // User already registered, return success with flag
                    response.setJoined(false);
                    response.setMessage("User already registered in this tournament");
                    return ResponseEntity.ok(response);
                }
            }

            tournament.getUsers().add(participantUser);
            casualRepository.save(tournament);

            response.setJoined(true);
            response.setMessage("Successfully joined Casual tournament");

        } else {
            throw new IllegalArgumentException("Unknown tournament type");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/unjoin")
    public ResponseEntity<Void> unjoinTournament(@PathVariable String id, @RequestParam Long userId) {
        // Look up the parent Tournament to determine type
        Tournament parent = tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (parent.getType() == TournamentType.SWISS) {
            SwissTournament tournament = swissTournamentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Swiss tournament details not found"));

            // Remove participant
            tournament.getParticipants().removeIf(p -> p.getUser().getTelegramId().equals(userId));
            swissTournamentRepository.save(tournament);

        } else if (parent.getType() == TournamentType.CASUAL) {
            TournamentCasual tournament = casualRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Casual tournament details not found"));

            User user = userRepository.findByTelegramId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            tournament.getUsers().remove(user);
            tournament.getUsersReady().remove(user);
            casualRepository.save(tournament);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/start-round")
    public ResponseEntity<Void> startRound(@PathVariable String id, @RequestParam Long requesterTelegramId) {
        swissTournamentService.startNextRound(id, requesterTelegramId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/next-round")
    public ResponseEntity<Void> startNextRound(@PathVariable String id, @RequestParam Long requesterTelegramId) {
        swissTournamentService.startNextRound(id, requesterTelegramId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitMatchResult(@PathVariable String id, @RequestBody SubmitMatchResultRequest request) {
        if (request.getScoreA() == null || request.getScoreB() == null) {
            throw new IllegalArgumentException("Both scores are required");
        }
        if (request.getSubmitterTelegramId() == null) {
            throw new IllegalArgumentException("Submitter telegramId is required");
        }
        swissTournamentService.submitMatchResult(id, request.getSubmitterTelegramId(), request.getScoreA(), request.getScoreB());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/standings")
    public ResponseEntity<List<StandingDTO>> getStandings(@PathVariable String id) {
        List<Participant> standings = swissTournamentService.getStandings(id);
        SwissTournament tournament = swissTournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        
        List<StandingDTO> result = new ArrayList<>();
        int rank = 1;
        for (Participant p : standings) {
            StandingDTO dto = new StandingDTO();
            dto.setParticipantId(p.getId());
            dto.setUserTag(p.getUser().getUserTag());
            dto.setDisplayName(p.getUser().getDisplayName());
            dto.setPoints(p.getPoints());
            dto.setTieBreaker(p.getTieBreaker());
            dto.setRank(rank++);
            
            // Calculate and set tiebreaker percentages
            dto.setOmwPercentage(swissTournamentService.getOMWPercentage(tournament, p));
            dto.setGwPercentage(swissTournamentService.getGWPercentage(tournament, p));
            dto.setOgwPercentage(swissTournamentService.getOGWPercentage(tournament, p));
            
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/pairings")
    public ResponseEntity<List<PairingDTO>> getCurrentPairings(@PathVariable String id) {
        List<Match> matches = swissTournamentService.getCurrentPairings(id);
        List<PairingDTO> result = new ArrayList<>();
        for (Match m : matches) {
            PairingDTO dto = new PairingDTO();
            dto.setMatchId(m.getId());
            dto.setPlayerA(m.getPlayerA() != null ? m.getPlayerA().getUser().getUserTag() : null);
            dto.setPlayerADisplayName(m.getPlayerA() != null ? m.getPlayerA().getUser().getDisplayName() : null);
            dto.setPlayerB(m.getPlayerB() != null ? m.getPlayerB().getUser().getUserTag() : null);
            dto.setPlayerBDisplayName(m.getPlayerB() != null ? m.getPlayerB().getUser().getDisplayName() : null);
            dto.setScoreA(m.getScoreA());
            dto.setScoreB(m.getScoreB());
            dto.setCompleted(m.isCompleted());
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantDTO>> getParticipants(@PathVariable String id) {
        List<Participant> participants = swissTournamentService.getParticipants(id);
        List<ParticipantDTO> result = new ArrayList<>();
        for (Participant p : participants) {
            ParticipantDTO dto = new ParticipantDTO();
            dto.setId(p.getId());
            dto.setPoints(p.getPoints());
            dto.setTieBreaker(p.getTieBreaker());
            UserDTO userDto = new UserDTO();
            userDto.setId(p.getUser().getId());
            userDto.setTelegramId(p.getUser().getTelegramId());
            userDto.setUserTag(p.getUser().getUserTag());
            userDto.setDisplayName(p.getUser().getDisplayName());
            userDto.setChatId(p.getUser().getChatId());
            dto.setUser(userDto);
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/remove-participant")
    public ResponseEntity<Void> removeParticipant(@PathVariable String id, @RequestParam Long userId, @RequestParam Long requesterTelegramId) {
        swissTournamentService.removeParticipant(id, userId, requesterTelegramId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/edit-result")
    public ResponseEntity<Void> editMatchResult(@PathVariable String id, @RequestBody SubmitMatchResultRequest request) {
        if (request.getMatchId() == null) {
            throw new IllegalArgumentException("Match ID is required");
        }
        if (request.getScoreA() == null || request.getScoreB() == null) {
            throw new IllegalArgumentException("Both scores are required");
        }
        if (request.getAdminId() == null) {
            throw new IllegalArgumentException("Admin ID is required");
        }
        swissTournamentService.editMatchResult(request.getMatchId(), request.getScoreA(), request.getScoreB(), request.getAdminId());
        return ResponseEntity.ok().build();
    }
}
