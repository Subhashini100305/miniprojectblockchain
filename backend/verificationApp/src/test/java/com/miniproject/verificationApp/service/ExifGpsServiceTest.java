package com.miniproject.verificationApp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExifGpsServiceTest {

    private final ExifGpsService service = new ExifGpsService();

    @Test
    void sameCoordinatesHaveZeroDistance() {
        assertEquals(
                0.0,
                service.calculateDistanceMeters(27.1751, 78.0421, 27.1751, 78.0421),
                0.001
        );
    }

    @Test
    void calculatesKnownOneDegreeLatitudeDistance() {
        assertEquals(
                111_195,
                service.calculateDistanceMeters(0.0, 0.0, 1.0, 0.0),
                100
        );
    }
}
