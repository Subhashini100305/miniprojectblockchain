package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.Review;
import com.miniproject.verificationApp.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewIntegrityServiceTest {

    @Mock
    private BlockchainService blockchainService;

    @InjectMocks
    private ReviewIntegrityService reviewIntegrityService;

    private Review buildBlockchainStoredReview() {
        User user = new User();
        user.setEmail("user@test.com");

        Review review = new Review();
        review.setUser(user);
        review.setReviewText("This place was absolutely wonderful.");
        review.setPlaceName("Taj Mahal");
        review.setStoredOnBlockchain(true);
        review.setBlockchainTxHash("0xTxHash");

        return review;
    }

    @Test
    void isBlockchainHashValid_returnsTrueWhenCurrentHashMatchesBlockchainHash() {
        Review review = buildBlockchainStoredReview();
        String expectedHash = HashUtil.sha256(
                review.getReviewText(),
                review.getPlaceName(),
                review.getUser().getEmail()
        );

        when(blockchainService.getStoredReviewHash("0xTxHash"))
                .thenReturn(Optional.of(expectedHash));

        assertTrue(reviewIntegrityService.isBlockchainHashValid(review));
    }

    @Test
    void isBlockchainHashValid_returnsFalseWhenCurrentHashDiffersFromBlockchainHash() {
        Review review = buildBlockchainStoredReview();

        when(blockchainService.getStoredReviewHash("0xTxHash"))
                .thenReturn(Optional.of("different-blockchain-hash"));

        assertFalse(reviewIntegrityService.isBlockchainHashValid(review));
    }

    @Test
    void isBlockchainHashValid_returnsFalseWhenBlockchainHashCannotBeRetrieved() {
        Review review = buildBlockchainStoredReview();

        when(blockchainService.getStoredReviewHash("0xTxHash"))
                .thenReturn(Optional.empty());

        assertFalse(reviewIntegrityService.isBlockchainHashValid(review));
    }

    @Test
    void isBlockchainHashValid_returnsFalseWhenBlockchainLookupFails() {
        Review review = buildBlockchainStoredReview();

        when(blockchainService.getStoredReviewHash("0xTxHash"))
                .thenThrow(new RuntimeException("connection failed"));

        assertFalse(reviewIntegrityService.isBlockchainHashValid(review));
    }
}
