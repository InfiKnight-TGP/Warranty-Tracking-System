package com.vault.service;

import com.vault.dto.RegisterRequest;
import com.vault.dto.UserResponse;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.exception.ConflictException;
import com.vault.exception.ResourceNotFoundException;
import com.vault.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new ConflictException("An account with this email already exists");
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()),
                request.phone() == null || request.phone().isBlank() ? null : request.phone().trim(), UserRole.USER);
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(), user.getCreatedAt());
    }
}
