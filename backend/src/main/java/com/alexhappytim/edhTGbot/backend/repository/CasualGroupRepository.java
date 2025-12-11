package com.alexhappytim.edhTGbot.backend.repository;

import com.alexhappytim.edhTGbot.backend.model.CasualGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CasualGroupRepository extends JpaRepository<CasualGroup, Long> {
    List<CasualGroup> findByTournamentId(String tournamentId);
    void deleteByTournamentId(String tournamentId);
}
