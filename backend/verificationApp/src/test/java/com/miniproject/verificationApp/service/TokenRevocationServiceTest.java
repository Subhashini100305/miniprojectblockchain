package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.RevokedToken;
import com.miniproject.verificationApp.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenRevocationService service;

    @Test
    void isRevoked_shouldReturnTrue_whenTokenIsRevoked() {

        when(jwtService.extractTokenId("token"))
                .thenReturn("jti-123");

        when(revokedTokenRepository.existsByJti("jti-123"))
                .thenReturn(true);

        assertTrue(service.isRevoked("token"));
    }

    @Test
    void isRevoked_shouldReturnFalse_whenTokenIsNotRevoked() {

        when(jwtService.extractTokenId("token"))
                .thenReturn("jti-123");

        when(revokedTokenRepository.existsByJti("jti-123"))
                .thenReturn(false);

        assertFalse(service.isRevoked("token"));
    }

    @Test
    void revoke_shouldSaveToken_whenTokenIsNotAlreadyRevoked() {

        String token = "token";
        String jti = "jti-123";

        when(jwtService.extractTokenId(token))
                .thenReturn(jti);

        Date expiration = new Date(
                System.currentTimeMillis() + 3600000
        );

        when(jwtService.extractExpiration(token))
                .thenReturn(expiration);

        when(revokedTokenRepository.existsByJti(jti))
                .thenReturn(false);

        service.revoke(token);

        verify(revokedTokenRepository)
                .deleteByExpiresAtBefore(any(LocalDateTime.class));

        verify(revokedTokenRepository)
                .save(any(RevokedToken.class));
    }

    @Test
    void revoke_shouldNotSave_whenTokenAlreadyRevoked() {

        String token = "token";
        String jti = "jti-123";

        when(jwtService.extractTokenId(token))
                .thenReturn(jti);

        when(revokedTokenRepository.existsByJti(jti))
                .thenReturn(true);

        service.revoke(token);

        verify(revokedTokenRepository)
                .deleteByExpiresAtBefore(any(LocalDateTime.class));

        verify(revokedTokenRepository, never())
                .save(any(RevokedToken.class));
    }
}