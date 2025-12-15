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
    private final SwissTournamentRepository swissTournamentRepository;
    private final ParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public void startNextRound(String tournamentId, Long requesterTelegramId) {
        SwissTournament tournament = swissTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Only owner can start rounds
        if (!tournament.getOwner().getTelegramId().equals(requesterTelegramId)) {
            throw new IllegalStateException("Only tournament owner can start rounds");
        }

        // If tournament already completed, cannot start more rounds
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new IllegalStateException("Tournament already completed");
        }

        // If there are existing rounds, ensure all matches are finished before starting next
        if (!tournament.getRounds().isEmpty()) {
            Round lastRound = tournament.getRounds().get(tournament.getRounds().size() - 1);
            boolean allFinished = lastRound.getMatches().stream().allMatch(Match::isCompleted);
            if (!allFinished) {
                throw new IllegalStateException("Cannot start next round: not all matches are finished");
            }

            // Prevent starting a new round if the last round was already the final one
            if (isLastRound(tournament)) {
                throw new IllegalStateException("Cannot start next round: last round already played");
            }
        } else {
            // Starting tournament should be possible only once: must be in REGISTRATION before first round
            if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
                throw new IllegalStateException("Cannot start tournament: invalid status");
            }
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
        swissTournamentRepository.save(tournament);
    }

    public boolean havePlayedBefore(Participant a, Participant b, SwissTournament tournament) {
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
    public void submitMatchResult(String tournamentId, Long submitterTelegramId, Integer scoreA, Integer scoreB) {
        SwissTournament tournament = swissTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        if (tournament.getRounds().isEmpty()) {
            throw new IllegalStateException("No rounds have been started yet");
        }

        validateBo3Score(scoreA, scoreB);

        Round currentRound = tournament.getRounds().get(tournament.getRounds().size() - 1);
        Match match = currentRound.getMatches().stream()
                .filter(m -> !m.isCompleted())
                .filter(m -> (m.getPlayerA() != null && m.getPlayerA().getUser().getTelegramId().equals(submitterTelegramId))
                        || (m.getPlayerB() != null && m.getPlayerB().getUser().getTelegramId().equals(submitterTelegramId)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Active match for player not found"));

        if (match.isCompleted()) {
            throw new IllegalStateException("Match already completed");
        }

        match.setScoreA(scoreA);
        match.setScoreB(scoreB);
        match.setCompleted(true);
        matchRepository.save(match);

        // Award points: 3 for win, 1 for tie, 0 for loss
        if (scoreA > scoreB) {
            match.getPlayerA().setPoints(match.getPlayerA().getPoints() + 3);
        } else if (scoreB > scoreA && match.getPlayerB() != null) {
            match.getPlayerB().setPoints(match.getPlayerB().getPoints() + 3);
        } else if (scoreA == scoreB) {
            // Tie: both get 1 point
            match.getPlayerA().setPoints(match.getPlayerA().getPoints() + 1);
            if (match.getPlayerB() != null) {
                match.getPlayerB().setPoints(match.getPlayerB().getPoints() + 1);
            }
        }

        participantRepository.save(match.getPlayerA());
        if (match.getPlayerB() != null) participantRepository.save(match.getPlayerB());

        boolean allDone = currentRound.getMatches().stream().allMatch(Match::isCompleted);
        if (allDone) {
            if (isLastRound(tournament)) {
                tournament.setStatus(TournamentStatus.COMPLETED);
                swissTournamentRepository.save(tournament);
            }
        }
    }

    private void validateBo3Score(Integer scoreA, Integer scoreB) {
        if (scoreA == null || scoreB == null) {
            throw new IllegalArgumentException("Scores are required");
        }
        if (scoreA < 0 || scoreB < 0 || scoreA > 2 || scoreB > 2) {
            throw new IllegalArgumentException("Scores must be between 0 and 2 for BO3");
        }
        boolean valid =
                (scoreA == 0 && scoreB == 0) ||
                (scoreA == 1 && scoreB == 0) ||
                (scoreA == 0 && scoreB == 1) ||
                (scoreA == 1 && scoreB == 1) ||
                (scoreA == 2 && scoreB == 0) ||
                (scoreA == 0 && scoreB == 2) ||
                (scoreA == 2 && scoreB == 1) ||
                (scoreA == 1 && scoreB == 2);
        if (!valid) {
            throw new IllegalArgumentException("Scores must be a valid BO3 result (0-0, 1-0, 0-1, 1-1, 2-0, 0-2, 2-1, 1-2)");
        }
    }

    public boolean isLastRound(SwissTournament tournament) {
        int numPlayers = tournament.getParticipants().size();
        int maxRounds = (int) Math.ceil(Math.log(numPlayers) / Math.log(2));
        return tournament.getRounds().size() >= maxRounds;
    }

    public List<Participant> getStandings(String tournamentId) {
        SwissTournament tournament = swissTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        List<Participant> participants = new ArrayList<>(tournament.getParticipants());
        
        // Calculate tiebreakers for each participant
        for (Participant p : participants) {
            calculateTiebreakers(tournament, p);
        }
        
        // Sort by points (desc), then by OMW% (desc), then by GW% (desc), then by OGW% (desc)
        participants.sort((p1, p2) -> {
            int pointsCmp = Integer.compare(p2.getPoints(), p1.getPoints());
            if (pointsCmp != 0) return pointsCmp;
            
            double omw1 = getOMWPercentage(tournament, p1);
            double omw2 = getOMWPercentage(tournament, p2);
            int omwCmp = Double.compare(omw2, omw1);
            if (omwCmp != 0) return omwCmp;
            
            double gw1 = getGWPercentage(tournament, p1);
            double gw2 = getGWPercentage(tournament, p2);
            int gwCmp = Double.compare(gw2, gw1);
            if (gwCmp != 0) return gwCmp;
            
            double ogw1 = getOGWPercentage(tournament, p1);
            double ogw2 = getOGWPercentage(tournament, p2);
            int ogwCmp = Double.compare(ogw2, ogw1);
            if (ogwCmp != 0) return ogwCmp;
            
            // If all tiebreakers are equal, use ID as final tiebreaker
            return p1.getId().compareTo(p2.getId());
        });
        
        return participants;
    }

    /**
     * Calculate Opponents' Match Win Percentage (OMW%)
     * The average of the Match Win Percentages of all opponents played against
     */
    public double getOMWPercentage(SwissTournament tournament, Participant participant) {
        List<Participant> opponents = getOpponents(tournament, participant);
        if (opponents.isEmpty()) return 0.0;
        
        double totalMWP = 0.0;
        for (Participant opponent : opponents) {
            totalMWP += getMatchWinPercentage(tournament, opponent);
        }
        return totalMWP / opponents.size();
    }

    /**
     * Calculate Game Win Percentage (GW%)
     * Total games won / Total games played (in BO3, count individual games)
     */
    public double getGWPercentage(SwissTournament tournament, Participant participant) {
        int gamesWon = 0;
        int gamesTotal = 0;
        
        for (Round round : tournament.getRounds()) {
            for (Match match : round.getMatches()) {
                if (match.isCompleted()) {
                    if (match.getPlayerA() != null && match.getPlayerA().getId().equals(participant.getId())) {
                        gamesWon += match.getScoreA();
                        gamesTotal += Math.max(match.getScoreA(), match.getScoreB()) + 
                                     Math.min(match.getScoreA(), match.getScoreB());
                    } else if (match.getPlayerB() != null && match.getPlayerB().getId().equals(participant.getId())) {
                        gamesWon += match.getScoreB();
                        gamesTotal += Math.max(match.getScoreA(), match.getScoreB()) + 
                                     Math.min(match.getScoreA(), match.getScoreB());
                    }
                }
            }
        }
        
        return gamesTotal == 0 ? 0.0 : (double) gamesWon / gamesTotal;
    }

    /**
     * Calculate Opponents' Game Win Percentage (OGW%)
     * The average of the Game Win Percentages of all opponents played against
     */
    public double getOGWPercentage(SwissTournament tournament, Participant participant) {
        List<Participant> opponents = getOpponents(tournament, participant);
        if (opponents.isEmpty()) return 0.0;
        
        double totalGWP = 0.0;
        for (Participant opponent : opponents) {
            totalGWP += getGWPercentage(tournament, opponent);
        }
        return totalGWP / opponents.size();
    }

    /**
     * Calculate Match Win Percentage (MWP)
     * Matches won / Matches played
     */
    public double getMatchWinPercentage(SwissTournament tournament, Participant participant) {
        int matchesWon = 0;
        int matchesTotal = 0;
        
        for (Round round : tournament.getRounds()) {
            for (Match match : round.getMatches()) {
                if (match.isCompleted()) {
                    if ((match.getPlayerA() != null && match.getPlayerA().getId().equals(participant.getId())) ||
                        (match.getPlayerB() != null && match.getPlayerB().getId().equals(participant.getId()))) {
                        matchesTotal++;
                        
                        if (match.getPlayerA() != null && match.getPlayerA().getId().equals(participant.getId())) {
                            if (match.getScoreA() > match.getScoreB()) {
                                matchesWon++;
                            }
                        } else if (match.getPlayerB() != null && match.getPlayerB().getId().equals(participant.getId())) {
                            if (match.getScoreB() > match.getScoreA()) {
                                matchesWon++;
                            }
                        }
                    }
                }
            }
        }
        
        return matchesTotal == 0 ? 0.0 : (double) matchesWon / matchesTotal;
    }

    /**
     * Get list of opponents this participant has played against
     */
    public List<Participant> getOpponents(SwissTournament tournament, Participant participant) {
        List<Participant> opponents = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        
        for (Round round : tournament.getRounds()) {
            for (Match match : round.getMatches()) {
                if (match.isCompleted()) {
                    if (match.getPlayerA() != null && match.getPlayerA().getId().equals(participant.getId()) &&
                        match.getPlayerB() != null) {
                        if (!seenIds.contains(match.getPlayerB().getId())) {
                            opponents.add(match.getPlayerB());
                            seenIds.add(match.getPlayerB().getId());
                        }
                    } else if (match.getPlayerB() != null && match.getPlayerB().getId().equals(participant.getId()) &&
                               match.getPlayerA() != null) {
                        if (!seenIds.contains(match.getPlayerA().getId())) {
                            opponents.add(match.getPlayerA());
                            seenIds.add(match.getPlayerA().getId());
                        }
                    }
                }
            }
        }
        
        return opponents;
    }

    private void calculateTiebreakers(SwissTournament tournament, Participant participant) {
        // Store OMW% as tieBreaker for now (backend use)
        // The API will provide detailed tiebreaker info
    }

    public List<Match> getCurrentPairings(String tournamentId) {
        SwissTournament tournament = swissTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        if (tournament.getRounds().isEmpty()) return Collections.emptyList();
        Round lastRound = tournament.getRounds().get(tournament.getRounds().size() - 1);
        return lastRound.getMatches();
    }

    @Transactional
    public void removeParticipant(String tournamentId, Long userId, Long requesterTelegramId) {
        SwissTournament tournament = swissTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Verify requester is tournament owner
        if (!tournament.getOwner().getTelegramId().equals(requesterTelegramId)) {
            throw new IllegalStateException("Only tournament owner can remove participants");
        }

        // Tournament must be in REGISTRATION status
        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new IllegalStateException("Cannot remove participants after tournament has started");
        }

        // Remove the participant by user ID (which is unique for each user, including temporary ones)
        tournament.getParticipants().removeIf(p -> {
            User user = p.getUser();
            // For regular users, match by telegramId
            // For temporary users (telegramId = -1), match by user database ID
            if (user.getTelegramId() == -1L) {
                return user.getId().equals(userId);
            } else {
                return user.getTelegramId().equals(userId);
            }
        });
        swissTournamentRepository.save(tournament);
    }

    @Transactional
    public void editMatchResult(Long matchId, Integer scoreA, Integer scoreB, Long requesterTelegramId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        SwissTournament tournament = match.getRound().getTournament();

        // Verify requester is tournament owner
        if (!tournament.getOwner().getTelegramId().equals(requesterTelegramId)) {
            throw new IllegalStateException("Only tournament owner can edit match results");
        }

        validateBo3Score(scoreA, scoreB);

        // Revert old points if match was already completed
        if (match.isCompleted()) {
            if (match.getScoreA() > match.getScoreB()) {
                match.getPlayerA().setPoints(match.getPlayerA().getPoints() - 3);
            } else if (match.getScoreB() > match.getScoreA()) {
                match.getPlayerB().setPoints(match.getPlayerB().getPoints() - 3);
            } else if (match.getScoreA() == match.getScoreB()) {
                // Revert tie: both had 1 point
                match.getPlayerA().setPoints(match.getPlayerA().getPoints() - 1);
                if (match.getPlayerB() != null) {
                    match.getPlayerB().setPoints(match.getPlayerB().getPoints() - 1);
                }
            }
        }

        // Set new scores
        match.setScoreA(scoreA);
        match.setScoreB(scoreB);
        match.setCompleted(true);

        // Award new points: 3 for win, 1 for tie, 0 for loss
        if (scoreA > scoreB) {
            match.getPlayerA().setPoints(match.getPlayerA().getPoints() + 3);
        } else if (scoreB > scoreA) {
            match.getPlayerB().setPoints(match.getPlayerB().getPoints() + 3);
        } else if (scoreA == scoreB) {
            // Tie: both get 1 point
            match.getPlayerA().setPoints(match.getPlayerA().getPoints() + 1);
            if (match.getPlayerB() != null) {
                match.getPlayerB().setPoints(match.getPlayerB().getPoints() + 1);
            }
        }

        matchRepository.save(match);
        participantRepository.save(match.getPlayerA());
        if (match.getPlayerB() != null) participantRepository.save(match.getPlayerB());
    }

    public List<Participant> getParticipants(String tournamentId) {
        SwissTournament tournament = swissTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
        return new ArrayList<>(tournament.getParticipants());
    }
}