package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.Review;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class TrustScoringService {

    public double calculateTrustPoints(Review review) {

        double aiScore = review.getAiConfidenceScore() == null
                ? 0
                : review.getAiConfidenceScore();

        int flagCount = review.getFlagCount() == null
                ? 0
                : review.getFlagCount();

        double disputeScore = Math.max(0, 100 - (flagCount * 25));

        long daysOld = Duration.between(
                review.getCreatedAt(),
                LocalDateTime.now()
        ).toDays();

        double timeScore = Math.max(0, 100 - daysOld);

        return (aiScore * 0.5)
                + (timeScore * 0.3)
                + (disputeScore * 0.2);
    }
}
