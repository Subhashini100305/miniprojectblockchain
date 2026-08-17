package com.miniproject.verificationApp.controller;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.service.AIVerificationResult;
import com.miniproject.verificationApp.service.AIVerificationService;
import com.miniproject.verificationApp.service.ExifGpsService;
import com.miniproject.verificationApp.service.JwtService;
import com.miniproject.verificationApp.service.TokenRevocationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GovernmentVerificationController.class)
@AutoConfigureMockMvc(addFilters = false)
// Redirect uploads to a local, writable test folder instead of the default C:/uploads/
@TestPropertySource(properties = "app.upload.dir=target/test-uploads/")
class GovernmentVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private GovernmentIdVerificationRepository verificationRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private AIVerificationService aiVerificationService;
    @MockBean private ExifGpsService exifGpsService;
    @MockBean private JwtService jwtService;
    @MockBean private com.miniproject.verificationApp.service.TokenRevocationService tokenRevocationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setName("Test User");

        when(jwtService.extractEmail("valid-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    }

    private MockMultipartFile sampleImage() {
        return new MockMultipartFile(
                "file",
                "proof.jpg",
                "image/jpeg",
                "fake-image-bytes".getBytes()
        );
    }

    @Test
    void upload_returnsVerified_whenAiAndGpsBothMatch() throws Exception {
        when(exifGpsService.extractGpsCoordinates(any()))
                .thenReturn(new double[]{27.1751, 78.0421}); // near Taj Mahal
        when(exifGpsService.calculateDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(50.0); // well within the 500m threshold

        when(aiVerificationService.verifyProof(any(), eq("Taj Mahal")))
                .thenReturn(AIVerificationResult.verified(
                        "Photo verified at Taj Mahal", 92.5f, "PHOTO", "Taj Mahal"));

        mockMvc.perform(multipart("/api/verification/upload")
                        .file(sampleImage())
                        .param("selectedPlace", "Taj Mahal")
                        .param("selectedLat", "27.1751")
                        .param("selectedLon", "78.0421")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.gpsVerified").value(true));

        verify(verificationRepository).save(any(GovernmentIdVerification.class));
        verify(userRepository).save(user);
    }

    @Test
    void upload_returnsRejected_whenAiVerificationFails() throws Exception {
        when(exifGpsService.extractGpsCoordinates(any())).thenReturn(null); // no GPS metadata

        when(aiVerificationService.verifyProof(any(), eq("Eiffel Tower")))
                .thenReturn(AIVerificationResult.rejected("No landmark detected"));

        mockMvc.perform(multipart("/api/verification/upload")
                        .file(sampleImage())
                        .param("selectedPlace", "Eiffel Tower")
                        .param("selectedLat", "48.8584")
                        .param("selectedLon", "2.2945")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.gpsReason").value("No GPS metadata found"));

        verify(verificationRepository).save(any(GovernmentIdVerification.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    void upload_returnsRejected_whenGpsMismatchDespiteAiVerified() throws Exception {
        when(exifGpsService.extractGpsCoordinates(any()))
                .thenReturn(new double[]{10.0, 10.0}); // far from claimed place
        when(exifGpsService.calculateDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(5000.0); // beyond 500m threshold

        when(aiVerificationService.verifyProof(any(), eq("Taj Mahal")))
                .thenReturn(AIVerificationResult.verified(
                        "Photo verified at Taj Mahal", 90f, "PHOTO", "Taj Mahal"));

        mockMvc.perform(multipart("/api/verification/upload")
                        .file(sampleImage())
                        .param("selectedPlace", "Taj Mahal")
                        .param("selectedLat", "27.1751")
                        .param("selectedLon", "78.0421")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.gpsVerified").value(false));
    }

    @Test
    void getStatus_returnsVerificationHistory() throws Exception {
        GovernmentIdVerification v = new GovernmentIdVerification();
        v.setId(1L);
        v.setUser(user);
        v.setPlaceName("Taj Mahal");
        v.setStatus(GovernmentIdVerification.Status.VERIFIED);
        v.setCreatedAt(LocalDateTime.now());

        when(verificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(v));

        mockMvc.perform(get("/api/verification/status")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeName").value("Taj Mahal"))
                .andExpect(jsonPath("$[0].status").value("VERIFIED"));
    }

    @Test
    void getStatus_returnsEmptyList_whenUserNotFound() throws Exception {
        when(jwtService.extractEmail("ghost-token")).thenReturn("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/verification/status")
                        .header("Authorization", "Bearer ghost-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}