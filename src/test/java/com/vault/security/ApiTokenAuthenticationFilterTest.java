package com.vault.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vault.entity.User;
import com.vault.entity.UserRole;
import com.vault.service.ApiTokenService;
import com.vault.service.ApiTokenService.AuthenticatedApiUser;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiTokenAuthenticationFilterTest {
    private final ApiTokenService tokenService = mock(ApiTokenService.class);
    private final ApiTokenAuthenticationFilter filter = new ApiTokenAuthenticationFilter(tokenService, new ObjectMapper().findAndRegisterModules());
    private final FilterChain chain = (request, response) -> { };

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void authenticatesValidBearerAndClearsContextAfterRequest() throws Exception {
        when(tokenService.authenticate("valid")).thenReturn(new AuthenticatedApiUser("demo@example.com", UserRole.USER));
        MockHttpServletRequest request = request("Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> username = new AtomicReference<>();
        FilterChain observingChain = (req, res) -> username.set(SecurityContextHolder.getContext().getAuthentication().getName());

        filter.doFilter(request, response, observingChain);

        assertEquals("demo@example.com", username.get());
        verify(tokenService).authenticate("valid");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rejectsInvalidBearerWith401() throws Exception {
        when(tokenService.authenticate("bad")).thenThrow(new ApiTokenService.InvalidApiTokenException());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("Bearer bad"), response, chain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void leavesRequestsWithoutBearerHeaderAlone() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(null), response, chain);
        verify(tokenService, never()).authenticate(org.mockito.ArgumentMatchers.anyString());
        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/purchases");
        if (authorization != null) request.addHeader("Authorization", authorization);
        return request;
    }
}
