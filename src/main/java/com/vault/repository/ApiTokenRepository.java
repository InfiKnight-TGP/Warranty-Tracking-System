package com.vault.repository;

import com.vault.entity.ApiToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
    Optional<ApiToken> findByTokenHashAndRevokedFalse(String tokenHash);
    Optional<ApiToken> findByTokenHash(String tokenHash);
}
