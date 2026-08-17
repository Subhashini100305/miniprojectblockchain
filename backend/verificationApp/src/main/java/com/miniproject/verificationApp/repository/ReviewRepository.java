package com.miniproject.verificationApp.repository;

import com.miniproject.verificationApp.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPlaceName(String placeName);

    // ✅ Search by place name
    List<Review> findByPlaceNameContainingIgnoreCase(String placeName);

    // ✅ Get all reviews by user
    List<Review> findByUserId(Long userId);

    // ✅ Only blockchain stored reviews for a place
    List<Review> findByPlaceNameContainingIgnoreCaseAndStoredOnBlockchainTrue(
            String placeName
    );

    // ✅ My reviews only if stored on blockchain
    List<Review> findByUserIdAndStoredOnBlockchainTrue(Long userId);

    // ✅ Place reviews sorted by trust points
    Page<Review>
    findByPlaceNameContainingIgnoreCaseAndStoredOnBlockchainTrueOrderByTrustPointsDesc(
            String placeName,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Review r
            set r.flagCount = coalesce(r.flagCount, 0) + 1
            where r.id = :reviewId
            """)
    int incrementFlagCount(@Param("reviewId") Long reviewId);
}