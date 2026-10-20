package com.vault.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vault.entity.ApiToken;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.repository.ApiTokenRepository;
import com.vault.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {
    @Mock private ApiTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ApiTokenService service;

    @Test
    void createsUrlSafeRandomTokenAndPersistsOnlyItsSha256Hash() {
        User user = new User("Demo", "demo@example.com", "encoded", null, UserRole.USER);
        when(tokenRepository.save(any(ApiToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = service.issue(user);

        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        ArgumentCaptor<ApiToken> saved = ArgumentCaptor.forClass(ApiToken.class);
        verify(tokenRepository).save(saved.capture());
        assertNotEquals(token, saved.getValue().getTokenHash());
        assertTrue(saved.getValue().getTokenHash().matches("[a-f0-9]{64}"));
        assertTrue(ApiTokenService.hash(token).equals(saved.getValue().getTokenHash()));
    }

    @Test
    void hashesSameTokenDeterministically() {
        assertTrue(ApiTokenService.hash("sample").equals(ApiTokenService.hash("sample")));
        assertNotEquals(ApiTokenService.hash("sample"), ApiTokenService.hash("different"));
    }
}
