package com.miniproject.verificationApp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OCRServiceTest {

    private final OCRService service = new OCRService();

    @Test
    void isValidTravelProof_shouldReturnFalse_whenTextIsNull() {

        assertFalse(
                service.isValidTravelProof(null, "Agra")
        );
    }

    @Test
    void isValidTravelProof_shouldReturnFalse_whenTextIsEmpty() {

        assertFalse(
                service.isValidTravelProof("", "Agra")
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenPlaceIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Welcome to Agra",
                        "Agra"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenTicketIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Travel ticket",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenBookingIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Hotel booking confirmation",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenHotelIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Hotel reservation",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenTravelIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Travel document",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenTourIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Tour package",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnTrue_whenVisitIsPresent() {

        assertTrue(
                service.isValidTravelProof(
                        "Visit permit",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldReturnFalse_whenNoTravelInformationExists() {

        assertFalse(
                service.isValidTravelProof(
                        "This is just random text",
                        "Bangalore"
                )
        );
    }

    @Test
    void isValidTravelProof_shouldBeCaseInsensitive() {

        assertTrue(
                service.isValidTravelProof(
                        "HOTEL BOOKING CONFIRMATION",
                        "Bangalore"
                )
        );
    }
}