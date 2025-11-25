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
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final SwissTournamentService swissTournamentService;

    @PostMapping
    public ResponseEntity<TournamentDTO> createTournament(@RequestBody CreateTournamentRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tournament name is required");
        }
        if (request.getMaxPlayers() < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 players");
        }
        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .maxPlayers(request.getMaxPlayers())
                .status(TournamentStatus.REGISTRATION)
                .owner(userRepository.findByTelegramId(request.getOwnerId()).get())
                .build();
        tournament = tournamentRepository.save(tournament);
        TournamentDTO dto = new TournamentDTO();
        dto.setId(tournament.getId());
        dto.setName(tournament.getName());
        dto.setMaxPlayers(tournament.getMaxPlayers());
        dto.setStatus(tournament.getStatus().name());
        dto.setParticipants(new ArrayList<>());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ParticipantDTO> addParticipant(@PathVariable Long id, @RequestBody JoinTournamentRequest request) {
//        Optional<Tournament> tournamentOpt = tournamentRepository.findById(id);
//        Optional<User> userOpt = userRepository.findByTelegramId(request.getUserId());
//        if (tournamentOpt.isEmpty()) {
//            throw new IllegalArgumentException("Tournament not found");
//        }
//        if (userOpt.isEmpty()) {
//            throw new IllegalArgumentException("User not found");
//        }
//        if (request.getIsTemporary()){
//           if(!Objects.equals(tournamentOpt.get().getOwner().getId(), userOpt.get().getId())){
//               throw new IllegalArgumentException("It is not your tournament");
//           }
//        }
//        Tournament tournament = tournamentOpt.get();
//        User user = userOpt.get();
//        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
//            throw new IllegalStateException("Cannot join: Tournament is not in registration phase");
//        }
//        if (tournament.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(user.getId())) && !request.getIsTemporary()) {
//            throw new IllegalStateException("User already registered in this tournament");
//        }
////        if (tournament.getParticipants().stream().anyMatch(p -> p.getUser().getDisplayName().equals(user.getId())) && !request.getIsTryingToAdd()) {
////            throw new IllegalStateException("User already registered in this tournament");
////        }
//        if (tournament.getParticipants().size() >= tournament.getMaxPlayers()) {
//            throw new IllegalStateException("Tournament is full");
//        }
//        Participant participant = Participant.builder()
//                .tournament(tournament)
//                .points(0)
//                .tieBreaker(0)
//                .build();
//        if (!request.getIsTryingToAdd()){
//            participant.setUser(user);
//        }
//        else {
//            User tempUser = User.builder()
//                    .userTag("")
//                    .displayName(request.getParticipantName())
//                    .telegramId((long) -1)
//                    .chatId((long) -1)
//                    .build();
//            tempUser = userRepository.save(tempUser);
//            participant.setUser(tempUser);
//        }
//        participant = participantRepository.save(participant);
//        ParticipantDTO dto = new ParticipantDTO();
//        UserDTO userDTO = new UserDTO();
//        userDTO.setId(user.getId());
//        userDTO.setUserTag(user.getUserTag());
//        userDTO.setDisplayName(user.getDisplayName());
//        dto.setId(participant.getId());
//        dto.setUser(userDTO);
//        dto.setPoints(0);
//        dto.setTieBreaker(0);
        return ResponseEntity.ok(new ParticipantDTO());
    }


    @PostMapping("/{id}/start-round")
    public ResponseEntity<Void> startNextRound(@PathVariable Long id) {
        swissTournamentService.startNextRound(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitMatchResult(@PathVariable Long id, @RequestBody SubmitMatchResultRequest request) {
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
    public ResponseEntity<List<StandingDTO>> getStandings(@PathVariable Long id) {
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
    public ResponseEntity<List<PairingDTO>> getCurrentPairings(@PathVariable Long id) {
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
