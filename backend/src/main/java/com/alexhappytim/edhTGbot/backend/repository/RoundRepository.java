package com.alexhappytim.edhTGbot.backend.repository;

import com.alexhappytim.edhTGbot.backend.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<Round, Long> {
}
