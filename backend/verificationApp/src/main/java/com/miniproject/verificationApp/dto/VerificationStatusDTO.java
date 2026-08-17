package com.miniproject.verificationApp.dto;

import com.miniproject.verificationApp.model.GovernmentIdVerification;

import java.time.LocalDateTime;

public class VerificationStatusDTO {

    private final Long id;
    private final GovernmentIdVerification.Status status;
    private final String placeName;
    private final Double aiConfidenceScore;
    private final Boolean gpsVerified;
    private final Double distanceMeters;
    private final LocalDateTime verifiedOn;
    private final LocalDateTime createdAt;

    public VerificationStatusDTO(GovernmentIdVerification verification) {
        this.id = verification.getId();
        this.status = verification.getStatus();
        this.placeName = verification.getPlaceName();
        this.aiConfidenceScore = verification.getAiConfidenceScore();
        this.gpsVerified = verification.getGpsVerified();
        this.distanceMeters = verification.getDistanceMeters();
        this.verifiedOn = verification.getVerifiedOn();
        this.createdAt = verification.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public GovernmentIdVerification.Status getStatus() {
        return status;
    }

    public String getPlaceName() {
        return placeName;
    }

    public Double getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public Boolean getGpsVerified() {
        return gpsVerified;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public LocalDateTime getVerifiedOn() {
        return verifiedOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
