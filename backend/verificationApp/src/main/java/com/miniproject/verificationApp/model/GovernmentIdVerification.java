package com.miniproject.verificationApp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "government_id_verifications")
public class GovernmentIdVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Foreign key to users table
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @JsonIgnore
    @Column(name = "proof_url", length = 1024)
    private String proofUrl;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    @Column(name = "verified_on")
    private LocalDateTime verifiedOn;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "place_name")
    private String placeName;

    @JsonIgnore
    @Column(name = "photo_latitude")
    private Double photoLatitude;

    @JsonIgnore
    @Column(name = "photo_longitude")
    private Double photoLongitude;

    @Column(name = "gps_verified")
    private Boolean gpsVerified = false;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    public enum Status {
        PENDING, VERIFIED, REJECTED
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Double aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public LocalDateTime getVerifiedOn() {
        return verifiedOn;
    }

    public void setVerifiedOn(LocalDateTime verifiedOn) {
        this.verifiedOn = verifiedOn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public Double getPhotoLatitude() {
        return photoLatitude;
    }

    public void setPhotoLatitude(Double photoLatitude) {
        this.photoLatitude = photoLatitude;
    }

    public Double getPhotoLongitude() {
        return photoLongitude;
    }

    public void setPhotoLongitude(Double photoLongitude) {
        this.photoLongitude = photoLongitude;
    }

    public Boolean getGpsVerified() {
        return gpsVerified;
    }

    public void setGpsVerified(Boolean gpsVerified) {
        this.gpsVerified = gpsVerified;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}