package com.alexhappytim.edhTGbot.backend.controller;

import com.alexhappytim.edhTGbot.backend.model.*;
import com.alexhappytim.edhTGbot.backend.repository.*;
import com.alexhappytim.edhTGbot.backend.service.SwissTournamentService;
import com.alexhappytim.mtg.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping("/tournaments/{id}/swiss")
@RequiredArgsConstructor
public class SwissTournamentController {
    private final SwissTournamentRepository swissTournamentRepository;
    private final SwissTournamentService swissTournamentService;

    @PostMapping("/start-round")
    public ResponseEntity<Void> startRound(@PathVariable String id, @RequestParam Long requesterTelegramId) {
        swissTournamentService.startNextRound(id, requesterTelegramId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/next-round")
    public ResponseEntity<Void> startNextRound(@PathVariable String id, @RequestParam Long requesterTelegramId) {
        swissTournamentService.startNextRound(id, requesterTelegramId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/submit")
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

    @GetMapping("/standings")
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

    @GetMapping("/pairings")
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

    @GetMapping("/participants")
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

    @PostMapping("/remove-participant")
    public ResponseEntity<Void> removeParticipant(@PathVariable String id, @RequestParam Long userId, @RequestParam Long requesterTelegramId) {
        swissTournamentService.removeParticipant(id, userId, requesterTelegramId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/edit-result")
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
