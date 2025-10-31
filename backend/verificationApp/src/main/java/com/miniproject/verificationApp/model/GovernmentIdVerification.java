package com.miniproject.verificationApp.model;

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

    @Column(name = "proof_url", length = 1024)
    private String proofUrl;

    @Column(name = "verified_on")
    private LocalDateTime verifiedOn;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // 🆕 New field for place name
    @Column(name = "place_name")
    private String placeName;

    public enum Status {
        PENDING, VERIFIED, REJECTED
    }

    // Getters & Setters
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
}
