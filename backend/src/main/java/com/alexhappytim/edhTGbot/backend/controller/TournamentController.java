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
            // Delegate to Swiss tournament join logic
            SwissTournament tournament = swissTournamentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Swiss tournament details not found"));

            User user = userRepository.findByTelegramId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
                throw new IllegalStateException("Cannot join: Tournament is not in registration phase");
            }

            if (tournament.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(user.getId()))) {
                throw new IllegalStateException("User already registered in this tournament");
            }

            if (tournament.getParticipants().size() >= tournament.getMaxPlayers()) {
                throw new IllegalStateException("Tournament is full");
            }

            Participant participant = Participant.builder()
                    .tournament(tournament)
                    .user(user)
                    .points(0)
                    .tieBreaker(0)
                    .build();
            participant = participantRepository.save(participant);
            tournament.getParticipants().add(participant);
            swissTournamentRepository.save(tournament);

            response.setJoined(true);
            response.setMessage("Successfully joined Swiss tournament");

        } else if (parent.getType() == TournamentType.CASUAL) {
            // Delegate to Casual tournament join logic
            TournamentCasual tournament = casualRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Casual tournament details not found"));

            User user = userRepository.findByTelegramId(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (tournament.getUsers().contains(user)) {
                throw new IllegalStateException("User already registered in this tournament");
            }

            tournament.getUsers().add(user);
            casualRepository.save(tournament);

            response.setJoined(true);
            response.setMessage("Successfully joined Casual tournament");

        } else {
            throw new IllegalArgumentException("Unknown tournament type");
        }

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{id}/start-round")
    public ResponseEntity<Void> startNextRound(@PathVariable String id) {
        swissTournamentService.startNextRound(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitMatchResult(@PathVariable String id, @RequestBody SubmitMatchResultRequest request) {
        if (request.getMatchId() == null) {
            throw new IllegalArgumentException("Match ID is required");
        }
        if (request.getScoreA() == null || request.getScoreB() == null) {
            throw new IllegalArgumentException("Both scores are required");
        }
        swissTournamentService.submitMatchResult(request.getMatchId(), request.getScoreA(), request.getScoreB());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/standings")
    public ResponseEntity<List<StandingDTO>> getStandings(@PathVariable String id) {
        List<Participant> standings = swissTournamentService.getStandings(id);
        List<StandingDTO> result = new ArrayList<>();
        int rank = 1;
        for (Participant p : standings) {
            StandingDTO dto = new StandingDTO();
            dto.setParticipantId(p.getId());
            dto.setUserTag(p.getUser().getUserTag());
            dto.setPoints(p.getPoints());
            dto.setTieBreaker(p.getTieBreaker());
            dto.setRank(rank++);
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
            dto.setPlayerB(m.getPlayerB() != null ? m.getPlayerB().getUser().getUserTag() : null);
            dto.setScoreA(m.getScoreA());
            dto.setScoreB(m.getScoreB());
            dto.setCompleted(m.isCompleted());
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }
}
