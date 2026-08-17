package com.miniproject.verificationApp.service;

import com.miniproject.verificationApp.model.Review;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReviewIntegrityService {

    private static final Logger logger =
            LoggerFactory.getLogger(ReviewIntegrityService.class);

    private final BlockchainService blockchainService;

    public ReviewIntegrityService(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    public boolean isBlockchainHashValid(Review review) {

        // -------------------------------------------------
        // 1. Basic validation
        // -------------------------------------------------
        if (review == null) {
            return false;
        }

        if (!review.getStoredOnBlockchain()) {
            return false;
        }

        if (review.getBlockchainTxHash() == null
                || review.getBlockchainTxHash().isBlank()
                || "pending".equalsIgnoreCase(review.getBlockchainTxHash())) {
            return false;
        }

        if (review.getReviewText() == null
                || review.getPlaceName() == null
                || review.getUser() == null
                || review.getUser().getEmail() == null) {
            return false;
        }

        try {

            // -------------------------------------------------
            // 2. Recalculate hash from CURRENT database data
            // -------------------------------------------------
            String currentHash = HashUtil.sha256(
                    review.getReviewText(),
                    review.getPlaceName(),
                    review.getUser().getEmail()
            );

            // -------------------------------------------------
            // 3. Compare with hash stored in database
            // -------------------------------------------------
            if (review.getReviewHash() == null
                    || !currentHash.equalsIgnoreCase(review.getReviewHash())) {

                logger.warn(
                        "Review database hash mismatch reviewId={}",
                        review.getId()
                );

                return false;
            }

            // -------------------------------------------------
            // 4. Get ORIGINAL hash from blockchain
            //
            // blockchainTxHash is the transaction hash:
            // 0x74eda547...
            //
            // getStoredReviewHash() extracts the review hash:
            // a6b678507...
            // -------------------------------------------------
            Optional<String> blockchainHash =
                    blockchainService.getStoredReviewHash(
                            review.getBlockchainTxHash()
                    );

            if (blockchainHash.isEmpty()) {

                logger.warn(
                        "No blockchain review hash found reviewId={} txHash={}",
                        review.getId(),
                        review.getBlockchainTxHash()
                );

                return false;
            }

            // -------------------------------------------------
            // 5. Compare CURRENT database hash
            //    against ORIGINAL blockchain hash
            // -------------------------------------------------
            boolean valid =
                    blockchainHash
                            .get()
                            .equalsIgnoreCase(currentHash);

            if (!valid) {

                logger.warn(
                        "BLOCKCHAIN INTEGRITY FAILURE reviewId={} txHash={}",
                        review.getId(),
                        review.getBlockchainTxHash()
                );

                return false;
            }

            // -------------------------------------------------
            // 6. Everything matches
            // -------------------------------------------------
            logger.debug(
                    "Review integrity verified reviewId={}",
                    review.getId()
            );

            return true;

        } catch (Exception e) {

            logger.warn(
                    "Review integrity verification failed reviewId={} exceptionType={}",
                    review.getId(),
                    e.getClass().getName()
            );

            return false;
        }
    }
}