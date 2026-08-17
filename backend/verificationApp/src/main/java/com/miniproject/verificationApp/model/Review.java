package com.miniproject.verificationApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "flag_count")
    private Integer flagCount = 0;

    @Column(name = "is_disputed")
    private Boolean isDisputed = false;

    @Column(columnDefinition = "TEXT")
    private String reviewText;

    private String placeName;

    private String reviewHash;

    private Integer rating;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "stored_on_blockchain")
    private Boolean storedOnBlockchain = false;

    @Column(name = "blockchain_tx_hash")
    private String blockchainTxHash;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore = 0.0;

    @Column(name = "trust_points")
    private Double trustPoints = 0.0;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getReviewHash() {
        return reviewHash;
    }

    public void setReviewHash(String reviewHash) {
        this.reviewHash = reviewHash;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getFlagCount() {
        return flagCount == null ? 0 : flagCount;
    }

    public void setFlagCount(Integer flagCount) {
        this.flagCount = flagCount;
    }

    public Boolean getIsDisputed() {
        return isDisputed != null && isDisputed;
    }

    public void setIsDisputed(Boolean isDisputed) {
        this.isDisputed = isDisputed;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Boolean getStoredOnBlockchain() {
        return storedOnBlockchain != null && storedOnBlockchain;
    }

    public void setStoredOnBlockchain(Boolean storedOnBlockchain) {
        this.storedOnBlockchain = storedOnBlockchain;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public Double getAiConfidenceScore() {
        return aiConfidenceScore == null ? 0.0 : aiConfidenceScore;
    }

    public void setAiConfidenceScore(Double aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public Double getTrustPoints() {
        return trustPoints == null ? 0.0 : trustPoints;
    }

    public void setTrustPoints(Double trustPoints) {
        this.trustPoints = trustPoints;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}