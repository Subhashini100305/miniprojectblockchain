package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTrustScoringServiceTest {

    @Mock
    private GovernmentIdVerificationRepository verificationRepository;

    @InjectMocks
    private UserTrustScoringService service;

    @Mock
    private User user;

    @Mock
    private GovernmentIdVerification verification;

    @Test
    void shouldReturnZeroWhenEmailNotVerifiedAndNoVerifiedId() {

        when(user.getEmailVerified()).thenReturn(false);
        when(user.getId()).thenReturn(1L);

        when(verificationRepository.findByUserIdAndStatus(
                1L,
                GovernmentIdVerification.Status.VERIFIED
        )).thenReturn(List.of());

        double result = service.calculateUserTrustScore(user);

        assertEquals(0.0, result);
    }

    @Test
    void shouldGiveEmailVerificationPoints() {

        when(user.getEmailVerified()).thenReturn(true);
        when(user.getId()).thenReturn(1L);

        when(verificationRepository.findByUserIdAndStatus(
                1L,
                GovernmentIdVerification.Status.VERIFIED
        )).thenReturn(List.of());

        double result = service.calculateUserTrustScore(user);

        assertEquals(20.0, result);
    }

    @Test
    void shouldGivePointsForVerifiedGovernmentId() {

        when(user.getEmailVerified()).thenReturn(false);
        when(user.getId()).thenReturn(1L);

        when(verification.getAiConfidenceScore()).thenReturn(80.0);
        when(verification.getGpsVerified()).thenReturn(false);

        when(verificationRepository.findByUserIdAndStatus(
                1L,
                GovernmentIdVerification.Status.VERIFIED
        )).thenReturn(List.of(verification));

        double result = service.calculateUserTrustScore(user);

        // 30 for verified ID + 16 for 80% AI confidence
        assertEquals(46.0, result);
    }

    @Test
    void shouldGiveGpsVerificationPoints() {

        when(user.getEmailVerified()).thenReturn(false);
        when(user.getId()).thenReturn(1L);

        when(verification.getAiConfidenceScore()).thenReturn(80.0);
        when(verification.getGpsVerified()).thenReturn(true);

        when(verificationRepository.findByUserIdAndStatus(
                1L,
                GovernmentIdVerification.Status.VERIFIED
        )).thenReturn(List.of(verification));

        double result = service.calculateUserTrustScore(user);

        // 30 ID + 16 AI + 30 GPS
        assertEquals(76.0, result);
    }

    @Test
    void shouldHandleNullAiConfidenceScore() {

        when(user.getEmailVerified()).thenReturn(false);
        when(user.getId()).thenReturn(1L);

        when(verification.getAiConfidenceScore()).thenReturn(null);
        when(verification.getGpsVerified()).thenReturn(false);

        when(verificationRepository.findByUserIdAndStatus(
                1L,
                GovernmentIdVerification.Status.VERIFIED
        )).thenReturn(List.of(verification));

        double result = service.calculateUserTrustScore(user);

        assertEquals(30.0, result);
    }

    @Test
    void shouldCapScoreAt100() {

        when(user.getEmailVerified()).thenReturn(true);
        when(user.getId()).thenReturn(1L);

        when(verification.getAiConfidenceScore()).thenReturn(100.0);
        when(verification.getGpsVerified()).thenReturn(true);

        when(verificationRepository.findByUserIdAndStatus(
                1L,
                GovernmentIdVerification.Status.VERIFIED
        )).thenReturn(List.of(verification));

        double result = service.calculateUserTrustScore(user);

        assertEquals(100.0, result);
    }
}