package com.dentallab.repo;

import com.dentallab.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthTokenRepo extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);
}
