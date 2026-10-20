package com.vault.service;

import com.vault.entity.ApiToken;
import com.vault.entity.User;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.ApiTokenRepository;
import com.vault.repository.UserRepository;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ApiTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public ApiTokenService(ApiTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new ApiToken(hash(rawToken), user));
        return rawToken;
    }

    @Transactional
    public String issueForEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        return issue(user);
    }

    @Transactional
    public AuthenticatedApiUser authenticate(String rawToken) {
        ApiToken token = tokenRepository.findByTokenHashAndRevokedFalse(hash(rawToken))
                .orElseThrow(() -> new InvalidApiTokenException());
        token.setLastUsedAt(LocalDateTime.now());
        User user = token.getUser();
        return new AuthenticatedApiUser(user.getEmail(), user.getRole());
    }

    @Transactional
    public void revoke(String rawToken) {
        ApiToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("API token was not found"));
        token.setRevoked(true);
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public static class InvalidApiTokenException extends RuntimeException { }
    public record AuthenticatedApiUser(String email, com.vault.entity.UserRole role) { }
}
