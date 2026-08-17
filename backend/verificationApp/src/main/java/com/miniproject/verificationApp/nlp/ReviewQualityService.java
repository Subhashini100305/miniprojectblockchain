package com.miniproject.verificationApp.nlp;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewQualityService {

    private final SpamDetector spamDetector =
            new SpamDetector();

    private final SentimentAnalyzer sentimentAnalyzer;

    private final LocationKeywordChecker locationChecker =
            new LocationKeywordChecker();

    public ReviewQualityService(SentimentAnalyzer sentimentAnalyzer) {
        this.sentimentAnalyzer = sentimentAnalyzer;
    }

    public QualityResult analyze(String review, String place) {

        int score = 0;

        List<String> issues = new ArrayList<>();

        // =========================
        // WORD COUNT CHECK
        // =========================

        int wordCount = review.trim().split("\\s+").length;

        if(wordCount >= 15) {
            score += 30;
        } else {
            issues.add("Review too short (minimum 15 words required)");
        }

        // =========================
        // SPAM CHECK
        // =========================

        boolean spam = spamDetector.isSpam(review);

        if(!spam) {
            score += 20;
        } else {
            issues.add("Spam-like review detected");
        }

        // =========================
        // SENTIMENT CHECK
        // =========================

        String sentiment =
                sentimentAnalyzer.analyzeSentiment(review);

        if(!sentiment.equalsIgnoreCase("Neutral")) {
            score += 20;
        } else {
            issues.add("Weak or neutral sentiment detected");
        }

        // =========================
        // LOCATION CHECK
        // =========================

        boolean containsLocation =
                locationChecker.containsLocation(review, place);

        if(containsLocation) {
            score += 30;
        } else {
            issues.add("Tourist location not clearly mentioned");
        }

        // =========================
        // FINAL DECISION
        // =========================

        boolean accepted = score >= 70;

        return new QualityResult(
                score,
                accepted,
                issues
        );
    }
}