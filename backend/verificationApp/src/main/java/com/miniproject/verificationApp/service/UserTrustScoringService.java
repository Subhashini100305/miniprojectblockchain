package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserTrustScoringService {

    @Autowired
    private GovernmentIdVerificationRepository verificationRepository;

    public double calculateUserTrustScore(User user) {
        double score = 0;

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            score += 20;
        }

        List<GovernmentIdVerification> verifiedRecords =
                verificationRepository.findByUserIdAndStatus(
                        user.getId(),
                        GovernmentIdVerification.Status.VERIFIED
                );

        if (!verifiedRecords.isEmpty()) {
            score += 30;

            double avgConfidence = verifiedRecords.stream()
                    .mapToDouble(v -> v.getAiConfidenceScore() == null
                            ? 0
                            : v.getAiConfidenceScore())
                    .average()
                    .orElse(0);

            score += (avgConfidence / 100.0) * 20;

            boolean gpsVerified = verifiedRecords.stream()
                    .anyMatch(v -> Boolean.TRUE.equals(v.getGpsVerified()));

            if (gpsVerified) {
                score += 30;
            }
        }

        return Math.min(score, 100);
    }
}
