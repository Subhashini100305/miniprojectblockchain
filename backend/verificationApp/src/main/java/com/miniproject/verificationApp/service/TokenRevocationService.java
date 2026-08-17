package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.RevokedToken;
import com.miniproject.verificationApp.repository.RevokedTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    public TokenRevocationService(
            RevokedTokenRepository revokedTokenRepository,
            JwtService jwtService
    ) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
    }

    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByJti(jwtService.extractTokenId(token));
    }

    @Transactional
    public void revoke(String token) {
        revokedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        String jti = jwtService.extractTokenId(token);
        if (!revokedTokenRepository.existsByJti(jti)) {
            LocalDateTime expiresAt = jwtService.extractExpiration(token)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            revokedTokenRepository.save(new RevokedToken(jti, expiresAt));
        }
    }
}
