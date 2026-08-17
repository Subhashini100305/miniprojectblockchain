package com.miniproject.verificationApp.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "review_flags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_flags_review_user",
                columnNames = {"review_id", "user_id"}
        )
)
public class ReviewFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public ReviewFlag() {}

    public ReviewFlag(Long reviewId, Long userId) {
        this.reviewId = reviewId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}