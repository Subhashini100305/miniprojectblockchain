package com.miniproject.verificationApp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService service;

    @BeforeEach
    void setUp() {

        service = new JwtService();

        ReflectionTestUtils.setField(
                service,
                "secret",
                "this-is-a-test-secret-key-that-is-long-enough-for-hs256"
        );

        ReflectionTestUtils.setField(
                service,
                "expiration",
                3600000L
        );
    }

    @Test
    void generateToken_shouldCreateValidToken() {

        String token =
                service.generateToken("test@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmail_shouldReturnEmail() {

        String token =
                service.generateToken("test@example.com");

        assertEquals(
                "test@example.com",
                service.extractEmail(token)
        );
    }

    @Test
    void extractTokenId_shouldReturnTokenId() {

        String token =
                service.generateToken("test@example.com");

        String tokenId =
                service.extractTokenId(token);

        assertNotNull(tokenId);
        assertFalse(tokenId.isBlank());
    }

    @Test
    void extractExpiration_shouldReturnFutureDate() {

        String token =
                service.generateToken("test@example.com");

        Date expiration =
                service.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(
                expiration.after(new Date())
        );
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {

        String token =
                service.generateToken("test@example.com");

        assertTrue(
                service.validateToken(token)
        );
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {

        assertFalse(
                service.validateToken("invalid-token")
        );
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {

        String token =
                service.generateToken("test@example.com");

        String tamperedToken =
                token.substring(0, token.length() - 2) + "ab";

        assertFalse(
                service.validateToken(tamperedToken)
        );
    }
}