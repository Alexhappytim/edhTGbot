package com.alexhappytim.edhTGbot.backend.repository;

import com.alexhappytim.edhTGbot.backend.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
}
