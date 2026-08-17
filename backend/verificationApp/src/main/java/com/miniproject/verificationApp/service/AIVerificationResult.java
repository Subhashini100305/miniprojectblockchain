
package com.miniproject.verificationApp.service;

public class AIVerificationResult {

    private boolean verified;
    private String message;
    private float confidenceScore;
    private String verificationType;
    private String detectedLandmark;
    private String aiProvider;

    private AIVerificationResult(
            boolean verified,
            String message,
            float confidenceScore,
            String verificationType,
            String detectedLandmark,
            String aiProvider
    ) {
        this.verified = verified;
        this.message = message;
        this.confidenceScore = confidenceScore;
        this.verificationType = verificationType;
        this.detectedLandmark = detectedLandmark;
        this.aiProvider = aiProvider;
    }

    public static AIVerificationResult verified(
            String message,
            float confidence,
            Object type,
            String landmark,
            String aiProvider
    ) {
        return new AIVerificationResult(
                true,
                message,
                confidence,
                type.toString(),
                landmark,
                aiProvider
        );
    }
    public static AIVerificationResult verified(
        String message,
        float confidence,
        Object type,
        String landmark
    ) {
        return new AIVerificationResult(
                true,
                message,
                confidence,
                type.toString(),
                landmark,
                "NONE"
        );
    }

    public static AIVerificationResult rejected(
            String message,
            String aiProvider
    ) {
        return new AIVerificationResult(
                false,
                message,
                0.0f,
                "REJECTED",
                null,
                aiProvider
        );
    }

    public static AIVerificationResult rejected(
            String message
    ) {
        return new AIVerificationResult(
                false,
                message,
                0.0f,
                "REJECTED",
                null,
                "NONE"
        );
    }

    public boolean isVerified() {
        return verified;
    }

    public String getMessage() {
        return message;
    }

    public float getConfidenceScore() {
        return confidenceScore;
    }

    public String getVerificationType() {
        return verificationType;
    }

    public String getDetectedLandmark() {
        return detectedLandmark;
    }

    public String getAiProvider() {
        return aiProvider;
    }
}
