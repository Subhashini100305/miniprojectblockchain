package com.miniproject.verificationApp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void producesStableSha256Hex() {
        String first = HashUtil.sha256("review", "place", "user@example.com");
        String second = HashUtil.sha256("review", "place", "user@example.com");

        assertEquals(first, second);
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]{64}"));
    }

    @Test
    void separatorsKeepFieldBoundariesDistinct() {
        assertNotEquals(
                HashUtil.sha256("ab", "c"),
                HashUtil.sha256("a", "bc")
        );
    }
}
