package com.miniproject.verificationApp.repository;

import com.miniproject.verificationApp.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    long deleteByExpiresAtBefore(LocalDateTime time);
}
