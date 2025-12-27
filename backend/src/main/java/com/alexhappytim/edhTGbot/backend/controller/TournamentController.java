package com.alexhappytim.edhTGbot.backend.controller;

import com.alexhappytim.edhTGbot.backend.model.*;
import com.alexhappytim.edhTGbot.backend.repository.*;
import com.alexhappytim.mtg.dto.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {
    private final TournamentRepository tournamentRepository;
    private final SwissTournamentRepository swissTournamentRepository;
    private final TournamentCasualRepository casualRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    
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

        Tournament parent = Tournament.builder()
                .type(TournamentType.SWISS)
                .build();
        parent = tournamentRepository.save(parent);

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
        Tournament parent = tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        JoinTournamentResponse response = new JoinTournamentResponse();
        response.setTournamentId(id);
        response.setTournamentType(parent.getType().name());

        if (parent.getType() == TournamentType.SWISS) {
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

                User owner = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
                        
                if (!tournament.getOwner().getId().equals(owner.getId())) {
                    throw new IllegalArgumentException("Only tournament owner can add temporary participants");
                }
                participantUser = userRepository.save(User.builder()
                        .userTag("")
                        .displayName(request.getParticipantName())
                        .telegramId(-1L)
                        .chatId(-1L)
                        .build());
            } else {
                participantUser = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                        
                if (tournament.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(participantUser.getId()))) {
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
            TournamentCasual tournament = casualRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Casual tournament details not found"));

            final User participantUser;
            
            if (request.getIsTemporary()) {
                User owner = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("Requester not found"));
                        
                if (!tournament.getOwner().getId().equals(owner.getId())) {
                    throw new IllegalArgumentException("Only tournament owner can add temporary participants");
                }
                participantUser = userRepository.save(User.builder()
                        .userTag("")
                        .displayName(request.getParticipantName())
                        .telegramId(-1L)
                        .chatId(-1L)
                        .build());
            } else {
                participantUser = userRepository.findByTelegramId(request.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                        
                if (tournament.getUsers().contains(participantUser)) {
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
        Tournament parent = tournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (parent.getType() == TournamentType.SWISS) {
            SwissTournament tournament = swissTournamentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Swiss tournament details not found"));
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

    @GetMapping("/by-owner/{ownerId}")
    public ResponseEntity<List<OwnerTournamentDTO>> getTournamentsByOwner(@PathVariable Long ownerId) {
        List<OwnerTournamentDTO> result = new ArrayList<>();
        
        // Fetch Swiss tournaments
        List<SwissTournament> swissTournaments = swissTournamentRepository.findAll().stream()
                .filter(t -> t.getOwner() != null && t.getOwner().getTelegramId().equals(ownerId))
                .collect(Collectors.toList());
        
        for (SwissTournament st : swissTournaments) {
            OwnerTournamentDTO dto = new OwnerTournamentDTO();
            dto.setId(st.getId());
            dto.setName(st.getName());
            dto.setType("SWISS");
            result.add(dto);
        }
        
        // Fetch Casual tournaments
        List<TournamentCasual> casualTournaments = casualRepository.findAll().stream()
                .filter(t -> t.getOwner() != null && t.getOwner().getTelegramId().equals(ownerId))
                .collect(Collectors.toList());
        
        for (TournamentCasual ct : casualTournaments) {
            OwnerTournamentDTO dto = new OwnerTournamentDTO();
            dto.setId(ct.getId());
            dto.setName(ct.getName());
            dto.setType("CASUAL");
            result.add(dto);
        }
        
        return ResponseEntity.ok(result);
    }
}
