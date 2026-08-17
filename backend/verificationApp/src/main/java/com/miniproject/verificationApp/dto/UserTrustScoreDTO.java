package com.miniproject.verificationApp.dto;

public class UserTrustScoreDTO {

    private double userTrustScore;
    private boolean emailVerified;
    private boolean photoVerified;
    private boolean gpsVerified;
    private double avgAiConfidence;

    public UserTrustScoreDTO(
            double userTrustScore,
            boolean emailVerified,
            boolean photoVerified,
            boolean gpsVerified,
            double avgAiConfidence
    ) {
        this.userTrustScore = userTrustScore;
        this.emailVerified = emailVerified;
        this.photoVerified = photoVerified;
        this.gpsVerified = gpsVerified;
        this.avgAiConfidence = avgAiConfidence;
    }

    public double getUserTrustScore() {
        return userTrustScore;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isPhotoVerified() {
        return photoVerified;
    }

    public boolean isGpsVerified() {
        return gpsVerified;
    }

    public double getAvgAiConfidence() {
        return avgAiConfidence;
    }
}
