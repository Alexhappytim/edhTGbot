package com.alexhappytim.edhTGbot.backend.service;

import com.alexhappytim.edhTGbot.backend.model.*;
import com.alexhappytim.edhTGbot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SwissTournamentService {
    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public void startNextRound(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new IllegalStateException("Tournament already completed");
        }
        List<Participant> participants = tournament.getParticipants();
        int roundNumber = tournament.getRounds().size() + 1;
        // Sort by points, then tieBreaker, then id
        participants.sort(Comparator.comparingInt(Participant::getPoints).reversed()
                .thenComparingInt(Participant::getTieBreaker).reversed()
                .thenComparing(Participant::getId));
        Set<Long> paired = new HashSet<>();
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            Participant a = participants.get(i);
            if (paired.contains(a.getId())) continue;
            // Find next opponent not already paired and not played before
            for (int j = i + 1; j < participants.size(); j++) {
                Participant b = participants.get(j);
                if (paired.contains(b.getId())) continue;
                if (!havePlayedBefore(a, b, tournament)) {
                    Match match = Match.builder()
                            .playerA(a)
                            .playerB(b)
                            .scoreA(null)
                            .scoreB(null)
                            .completed(false)
                            .build();
                    matches.add(match);
                    paired.add(a.getId());
                    paired.add(b.getId());
                    break;
                }
            }
        }
        // Handle bye (odd number of players)
        for (Participant p : participants) {
            if (!paired.contains(p.getId())) {
                Match bye = Match.builder()
                        .playerA(p)
                        .playerB(null)
                        .scoreA(1)
                        .scoreB(0)
                        .completed(true)
                        .build();
                matches.add(bye);
                paired.add(p.getId());
            }
        }
        Round round = Round.builder()
                .roundNumber(roundNumber)
                .tournament(tournament)
                .matches(new ArrayList<>())
                .build();
        round = roundRepository.save(round);
        for (Match m : matches) {
            m.setRound(round);
            matchRepository.save(m);
            round.getMatches().add(m);
        }
        tournament.getRounds().add(round);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);
    }

    public boolean havePlayedBefore(Participant a, Participant b, Tournament tournament) {
        for (Round round : tournament.getRounds()) {
            for (Match match : round.getMatches()) {
                if (match.getPlayerA() != null && match.getPlayerB() != null) {
                    if ((match.getPlayerA().getId().equals(a.getId()) && match.getPlayerB().getId().equals(b.getId())) ||
                        (match.getPlayerA().getId().equals(b.getId()) && match.getPlayerB().getId().equals(a.getId()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Transactional
    public void submitMatchResult(Long matchId, Integer scoreA, Integer scoreB) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if (match.isCompleted()) throw new IllegalStateException("Match already completed");
        match.setScoreA(scoreA);
        match.setScoreB(scoreB);
        match.setCompleted(true);
        matchRepository.save(match);
        // Update points
        if (scoreA > scoreB) {
            match.getPlayerA().setPoints(match.getPlayerA().getPoints() + 1);
        } else if (scoreB > scoreA) {
            match.getPlayerB().setPoints(match.getPlayerB().getPoints() + 1);
        } else {
            match.getPlayerA().setPoints(match.getPlayerA().getPoints() + 0);
            match.getPlayerB().setPoints(match.getPlayerB().getPoints() + 0);
        }
        participantRepository.save(match.getPlayerA());
        if (match.getPlayerB() != null) participantRepository.save(match.getPlayerB());
        // Check if all matches in round are completed
        Round round = match.getRound();
        boolean allDone = round.getMatches().stream().allMatch(Match::isCompleted);
        if (allDone) {
            Tournament tournament = round.getTournament();
            // If last round, mark as completed
            if (isLastRound(tournament)) {
                tournament.setStatus(TournamentStatus.COMPLETED);
                tournamentRepository.save(tournament);
            }
        }
    }

    public boolean isLastRound(Tournament tournament) {
        int numPlayers = tournament.getParticipants().size();
        int maxRounds = (int) Math.ceil(Math.log(numPlayers) / Math.log(2));
        return tournament.getRounds().size() >= maxRounds;
    }

    public List<Participant> getStandings(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        List<Participant> participants = new ArrayList<>(tournament.getParticipants());
        participants.sort(Comparator.comparingInt(Participant::getPoints).reversed()
                .thenComparingInt(Participant::getTieBreaker).reversed()
                .thenComparing(Participant::getId));
        return participants;
    }

    public List<Match> getCurrentPairings(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (tournament.getRounds().isEmpty()) return Collections.emptyList();
        Round lastRound = tournament.getRounds().get(tournament.getRounds().size() - 1);
        return lastRound.getMatches();
    }
}
