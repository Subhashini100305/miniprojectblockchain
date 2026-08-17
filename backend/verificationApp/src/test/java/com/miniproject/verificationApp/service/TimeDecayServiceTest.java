package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.Review;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeDecayServiceTest {

    private final TimeDecayService service = new TimeDecayService();

    @Test
    void calculateRawRating_shouldReturnZero_whenReviewsAreNull() {
        double result = service.calculateRawRating(null);

        assertEquals(0.0, result);
    }

    @Test
    void calculateRawRating_shouldReturnZero_whenReviewsAreEmpty() {
        double result = service.calculateRawRating(List.of());

        assertEquals(0.0, result);
    }

    @Test
    void calculateRawRating_shouldCalculateAverageRating() {
        Review review1 = mock(Review.class);
        Review review2 = mock(Review.class);
        Review review3 = mock(Review.class);

        when(review1.getRating()).thenReturn(5);
        when(review2.getRating()).thenReturn(4);
        when(review3.getRating()).thenReturn(3);

        double result = service.calculateRawRating(
                List.of(review1, review2, review3)
        );

        assertEquals(4.0, result);
    }

    @Test
    void calculateDecayedRating_shouldReturnZero_whenReviewsAreNull() {
        double result = service.calculateDecayedRating(null);

        assertEquals(0.0, result);
    }

    @Test
    void calculateDecayedRating_shouldReturnZero_whenReviewsAreEmpty() {
        double result = service.calculateDecayedRating(List.of());

        assertEquals(0.0, result);
    }

    @Test
    void calculateDecayedRating_shouldIgnoreReview_whenCreatedAtIsNull() {
        Review review = mock(Review.class);

        when(review.getRating()).thenReturn(5);
        when(review.getCreatedAt()).thenReturn(null);

        double result = service.calculateDecayedRating(List.of(review));

        assertEquals(0.0, result);
    }

    @Test
    void calculateDecayedRating_shouldCalculateRating_forValidReview() {
        Review review = mock(Review.class);

        when(review.getRating()).thenReturn(5);
        when(review.getCreatedAt()).thenReturn(LocalDateTime.now());

        double result = service.calculateDecayedRating(List.of(review));

        assertEquals(5.0, result, 0.0001);
    }

    @Test
    void calculateDecayedRating_shouldUseMultipleReviews() {
        Review recentReview = mock(Review.class);
        Review oldReview = mock(Review.class);

        when(recentReview.getRating()).thenReturn(5);
        when(recentReview.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(oldReview.getRating()).thenReturn(1);
        when(oldReview.getCreatedAt()).thenReturn(
                LocalDateTime.now().minusDays(30)
        );

        double result = service.calculateDecayedRating(
                List.of(recentReview, oldReview)
        );

        // Recent review has more weight, so result should be
        // closer to 5 than to 1.
        assertEquals(true, result > 3.0);
        assertEquals(true, result < 5.0);
    }
}