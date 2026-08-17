package com.miniproject.verificationApp.controller;

import com.miniproject.verificationApp.dto.VerificationResponseDTO;
import com.miniproject.verificationApp.dto.VerificationStatusDTO;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.service.AIVerificationService;
import com.miniproject.verificationApp.service.AIVerificationResult;
import com.miniproject.verificationApp.service.ExifGpsService;
import com.miniproject.verificationApp.service.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/verification")
@CrossOrigin(origins = "http://localhost:3000")
public class GovernmentVerificationController {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    GovernmentVerificationController.class
            );

    @Autowired
    private GovernmentIdVerificationRepository verificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIVerificationService aiVerificationService;

    @Autowired
    private ExifGpsService exifGpsService;

    @Autowired
    private JwtService jwtService;

    @Value("${app.upload.dir:C:/uploads/}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<VerificationResponseDTO> uploadProof(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("selectedPlace") String selectedPlace,
            @RequestParam("selectedLat") double selectedLat,
            @RequestParam("selectedLon") double selectedLon
    ) {

        String email =
                jwtService.extractEmail(
                        authHeader.substring(7)
                );

        Optional<User> userOpt =
                userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        try {

            // =================================================
            // SAVE UPLOADED FILE
            // =================================================

            File dir = new File(uploadDir);

            if (!dir.exists() && !dir.mkdirs()) {

                throw new IOException(
                        "Unable to create upload directory: "
                                + dir.getAbsolutePath()
                );
            }

            String fileName =
                    UUID.randomUUID().toString();

            String filePath =
                    new File(
                            dir,
                            fileName
                    ).getAbsolutePath();

            File uploadedFile =
                    new File(filePath);

            file.transferTo(uploadedFile);

            // =================================================
            // GPS VERIFICATION
            // =================================================

            double[] gpsCoordinates =
                    exifGpsService.extractGpsCoordinates(
                            uploadedFile
                    );

            boolean gpsVerified = false;

            double distanceMeters = -1;

            Double photoLat = null;
            Double photoLon = null;

            String gpsReason = "";

            if (gpsCoordinates != null) {

                photoLat =
                        gpsCoordinates[0];

                photoLon =
                        gpsCoordinates[1];

                distanceMeters =
                        exifGpsService.calculateDistanceMeters(
                                photoLat,
                                photoLon,
                                selectedLat,
                                selectedLon
                        );

                gpsVerified =
                        distanceMeters <= 500;

                if (!gpsVerified) {

                    gpsReason =
                            "Photo GPS does not match selected location";
                }

            } else {

                gpsReason =
                        "No GPS metadata found";
            }

            // =================================================
            // AI VERIFICATION
            //
            // AIVerificationService internally decides:
            //
            // Google Vision first
            //       ↓
            // Google works → Google result
            //       ↓
            // Google technical failure
            //       ↓
            // Azure Vision
            // =================================================

            AIVerificationResult aiResult =
                    aiVerificationService.verifyProof(
                            uploadedFile,
                            selectedPlace
                    );

            logger.info(
                    "AI provider used={}",
                    aiResult.getAiProvider()
            );

            // =================================================
            // CREATE VERIFICATION RECORD
            // =================================================

            GovernmentIdVerification verification =
                    new GovernmentIdVerification();

            verification.setUser(user);

            verification.setProofUrl(filePath);

            verification.setPlaceName(
                    selectedPlace
            );

            verification.setCreatedAt(
                    LocalDateTime.now()
            );

            verification.setPhotoLatitude(
                    photoLat
            );

            verification.setPhotoLongitude(
                    photoLon
            );

            verification.setGpsVerified(
                    gpsVerified
            );

            verification.setDistanceMeters(
                    distanceMeters
            );

            verification.setAiConfidenceScore(
                    (double)
                            aiResult.getConfidenceScore()
            );

            // =================================================
            // VERIFIED
            // =================================================

            if (aiResult.isVerified()
                    && (gpsVerified
                    || gpsCoordinates == null)) {

                verification.setStatus(
                        GovernmentIdVerification.Status.VERIFIED
                );

                verification.setVerifiedOn(
                        LocalDateTime.now()
                );

                user.setGovernmentIdVerified(
                        true
                );

                userRepository.save(user);

                verificationRepository.save(
                        verification
                );

                logger.info(
                        "Verification completed status=VERIFIED provider={} verificationType={}",
                        aiResult.getAiProvider(),
                        aiResult.getVerificationType()
                );

                VerificationResponseDTO response =
                        new VerificationResponseDTO(
                                "VERIFIED",
                                selectedPlace,
                                (double)
                                        aiResult
                                                .getConfidenceScore(),
                                aiResult
                                        .getVerificationType(),
                                aiResult
                                        .getAiProvider(),
                                gpsVerified,
                                distanceMeters,
                                null,
                                gpsReason
                        );

                return ResponseEntity.ok(
                        response
                );
            }

            // =================================================
            // REJECTED
            // =================================================

            verification.setStatus(
                    GovernmentIdVerification.Status.REJECTED
            );

            verificationRepository.save(
                    verification
            );

            String reason;

            if (!aiResult.isVerified()) {

                reason =
                        "AI could not verify this image. "
                                + aiResult.getMessage();

            } else if (
                    gpsCoordinates != null
                            && !gpsVerified
            ) {

                reason =
                        "GPS mismatch. Photo was taken "
                                + String.format(
                                "%.2f",
                                distanceMeters
                        )
                                + " meters away from selected place.";

            } else {

                reason =
                        "Verification failed";
            }

            logger.info(
                    "Verification completed status=REJECTED provider={} verificationType={}",
                    aiResult.getAiProvider(),
                    aiResult.getVerificationType()
            );

            VerificationResponseDTO response =
                    new VerificationResponseDTO(
                            "REJECTED",
                            selectedPlace,
                            (double)
                                    aiResult
                                            .getConfidenceScore(),
                            aiResult
                                    .getVerificationType(),
                            aiResult
                                    .getAiProvider(),
                            gpsVerified,
                            distanceMeters,
                            reason,
                            gpsReason
                    );

            return ResponseEntity
                    .badRequest()
                    .body(response);

        } catch (IOException e) {

            logger.error(
                    "Verification upload failed exceptionType={}",
                    e.getClass().getName()
            );

            throw new IllegalStateException(
                    "Verification upload failed"
            );
        }
    }

    // =========================================================
    // GET VERIFICATION STATUS
    // =========================================================

    @GetMapping("/status")
    public List<VerificationStatusDTO> getVerificationStatus(
            @RequestHeader("Authorization") String authHeader
    ) {

        String email =
                jwtService.extractEmail(
                        authHeader.substring(7)
                );

        Optional<User> userOpt =
                userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {

            return new ArrayList<>();
        }

        User user =
                userOpt.get();

        return verificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(VerificationStatusDTO::new)
                .toList();
    }
}