package com.miniproject.verificationApp.repository;

import com.miniproject.verificationApp.model.ReviewFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewFlagRepository extends JpaRepository<ReviewFlag, Long> {

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
}