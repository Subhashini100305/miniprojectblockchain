package com.miniproject.verificationApp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIVerificationResultTest {

    @Test
    void verified_shouldCreateVerifiedResult() {

        AIVerificationResult result =
                AIVerificationResult.verified(
                        "Photo verified",
                        95.0f,
                        "PHOTO",
                        "Taj Mahal"
                );

        assertTrue(result.isVerified());
        assertEquals("Photo verified", result.getMessage());
        assertEquals(95.0f, result.getConfidenceScore());
        assertEquals("PHOTO", result.getVerificationType());
        assertEquals("Taj Mahal", result.getDetectedLandmark());
    }

    @Test
    void rejected_shouldCreateRejectedResult() {

        AIVerificationResult result =
                AIVerificationResult.rejected(
                        "No landmark detected"
                );

        assertFalse(result.isVerified());
        assertEquals("No landmark detected", result.getMessage());
        assertEquals(0.0f, result.getConfidenceScore());
        assertEquals("REJECTED", result.getVerificationType());
        assertNull(result.getDetectedLandmark());
    }
}