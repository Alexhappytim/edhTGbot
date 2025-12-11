package com.alexhappytim.edhTGbot.backend.repository;

import com.alexhappytim.edhTGbot.backend.model.SwissTournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwissTournamentRepository extends JpaRepository<SwissTournament, String> {
}
