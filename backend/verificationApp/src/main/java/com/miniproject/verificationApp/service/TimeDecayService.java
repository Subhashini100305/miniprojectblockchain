package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.Review;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeDecayService {

    // Controls how fast old reviews lose importance
    private static final double LAMBDA = 0.05;

    // -----------------------------
    // RAW AVERAGE RATING
    // -----------------------------
    public double calculateRawRating(List<Review> reviews) {

        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        for (Review review : reviews) {
            total += review.getRating();
        }

        return total / reviews.size();
    }

    // -----------------------------
    // TIME-DECAY WEIGHTED RATING
    // -----------------------------
    public double calculateDecayedRating(List<Review> reviews) {

        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        LocalDateTime now = LocalDateTime.now();

        for (Review review : reviews) {

            // ✅ SAFETY CHECK (IMPORTANT FIX)
            if (review.getCreatedAt() == null) {
                continue;
            }

            // how old the review is (in days)
            long daysOld = Duration.between(
                    review.getCreatedAt(),
                    now
            ).toDays();

            // exponential decay formula
            double weight = Math.exp(-LAMBDA * daysOld);

            weightedSum += review.getRating() * weight;
            totalWeight += weight;
        }

        return totalWeight == 0 ? 0.0 : weightedSum / totalWeight;
    }
}