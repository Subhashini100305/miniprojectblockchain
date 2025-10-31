package com.miniproject.verificationApp.repository;

import com.miniproject.verificationApp.model.EmailVerificationToken;
import com.miniproject.verificationApp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(User user);
}
