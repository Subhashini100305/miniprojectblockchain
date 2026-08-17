package com.miniproject.verificationApp.nlp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewQualityServiceTest {

    private SentimentAnalyzer sentimentAnalyzer;
    private ReviewQualityService service;

    @BeforeEach
    void setUp() {
        sentimentAnalyzer = mock(SentimentAnalyzer.class);
        service = new ReviewQualityService(sentimentAnalyzer);
    }

    @Test
    void acceptsHighQualityRelevantReview() {
        String review = "My visit to Taj Mahal was absolutely wonderful with beautiful architecture peaceful gardens helpful guides and unforgettable views today";
        when(sentimentAnalyzer.analyzeSentiment(review)).thenReturn("Positive");

        QualityResult result = service.analyze(review, "Taj Mahal");

        assertTrue(result.isAccepted());
        assertEquals(100, result.getScore());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void rejectsShortNeutralIrrelevantReview() {
        String review = "brief review";
        when(sentimentAnalyzer.analyzeSentiment(review)).thenReturn("Neutral");

        QualityResult result = service.analyze(review, "Taj Mahal");

        assertFalse(result.isAccepted());
        assertEquals(20, result.getScore());
        assertEquals(3, result.getIssues().size());
    }
}
