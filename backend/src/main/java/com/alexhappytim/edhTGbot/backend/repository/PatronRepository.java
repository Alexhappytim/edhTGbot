package com.alexhappytim.edhTGbot.backend.repository;

import com.alexhappytim.edhTGbot.backend.model.Patron;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatronRepository extends JpaRepository<Patron, Long> {

    Optional<Object> findByDisplayName(String userTag);
}