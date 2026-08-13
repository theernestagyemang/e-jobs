package com.ejobs.portal.repository;

import com.ejobs.portal.model.PasswordResetToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /** Redemption path. The user is fetched with it - resetting always touches both. */
    @EntityGraph(attributePaths = "user")
    Optional<PasswordResetToken> findByToken(String token);
}
