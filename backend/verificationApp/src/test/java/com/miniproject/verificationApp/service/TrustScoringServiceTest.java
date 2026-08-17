package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.Review;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrustScoringServiceTest {

    private final TrustScoringService service = new TrustScoringService();

    @Test
    void calculatesTrustForNewUnflaggedReview() {
        Review review = new Review();
        review.setAiConfidenceScore(80.0);
        review.setFlagCount(0);
        review.setCreatedAt(LocalDateTime.now());

        assertEquals(90.0, service.calculateTrustPoints(review), 0.001);
    }

    @Test
    void flagsReduceDisputeComponent() {
        Review review = new Review();
        review.setAiConfidenceScore(80.0);
        review.setFlagCount(4);
        review.setCreatedAt(LocalDateTime.now());

        assertEquals(70.0, service.calculateTrustPoints(review), 0.001);
    }
}
