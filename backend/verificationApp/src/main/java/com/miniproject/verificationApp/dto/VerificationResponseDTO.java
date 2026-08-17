package com.miniproject.verificationApp.dto;

public class VerificationResponseDTO {

    private String status;
    private String place;
    private Double confidence;
    private String verificationType;
    private String aiProvider;
    private Boolean gpsVerified;
    private Double distanceMeters;
    private String reason;
    private String gpsReason;

    public VerificationResponseDTO(
            String status,
            String place,
            Double confidence,
            String verificationType,
            String aiProvider,
            Boolean gpsVerified,
            Double distanceMeters,
            String reason,
            String gpsReason
    ) {
        this.status = status;
        this.place = place;
        this.confidence = confidence;
        this.verificationType = verificationType;
        this.aiProvider = aiProvider;
        this.gpsVerified = gpsVerified;
        this.distanceMeters = distanceMeters;
        this.reason = reason;
        this.gpsReason = gpsReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getGpsReason() {
        return gpsReason;
    }

    public void setGpsReason(String gpsReason) {
        this.gpsReason = gpsReason;
    }
}