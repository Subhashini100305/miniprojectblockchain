package com.miniproject.verificationApp.controller;

import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.service.OCRService;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private GovernmentIdVerificationRepository verificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OCRService ocrService;

    private static final String UPLOAD_DIR = "C:/uploads/";

    // ✅ Upload and verify proof using OCR
    @PostMapping("/upload")
    public String uploadProof(
            @RequestParam("email") String email,
            @RequestParam("file") MultipartFile file,
            @RequestParam("selectedPlace") String selectedPlace
    ) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return "User not found!";
        }

        User user = userOpt.get();

        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            // Save uploaded file
            String filePath = UPLOAD_DIR + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File uploadedFile = new File(filePath);
            file.transferTo(uploadedFile);

            // 🧠 Extract text using OCR
            String extractedText = ocrService.extractText(uploadedFile);
            System.out.println("Extracted text: " + extractedText);
            System.out.println("Selected place: " + selectedPlace);

            GovernmentIdVerification verification = new GovernmentIdVerification();
            verification.setUser(user);
            verification.setProofUrl(filePath);
            verification.setPlaceName(selectedPlace);
            verification.setCreatedAt(LocalDateTime.now());

            if (extractedText != null && !extractedText.isEmpty()) {
                String normalizedText = extractedText.toLowerCase().replaceAll("[^a-z]", "");
                String normalizedPlace = selectedPlace.toLowerCase().replaceAll("[^a-z]", "");

                String[] placeParts = selectedPlace
                        .replaceAll("[^a-zA-Z ]", " ")
                        .toLowerCase()
                        .split("\\s+");

                boolean matchFound = false;
                for (String part : placeParts) {
                    if (part.length() < 3) continue;
                    if (normalizedText.contains(part) || extractedText.toLowerCase().contains(part)) {
                        matchFound = true;
                        break;
                    }
                }

                if (matchFound) {
                    verification.setStatus(GovernmentIdVerification.Status.VERIFIED);
                    verification.setVerifiedOn(LocalDateTime.now());
                    user.setGovernmentIdVerified(true);
                    userRepository.save(user);
                    verificationRepository.save(verification);
                    return "VERIFIED:" + selectedPlace;
                } else {
                    verification.setStatus(GovernmentIdVerification.Status.REJECTED);
                    verificationRepository.save(verification);
                    return "REJECTED:" + selectedPlace;
                }
            } else {
                verification.setStatus(GovernmentIdVerification.Status.REJECTED);
                verificationRepository.save(verification);
                return "NO_TEXT:" + selectedPlace;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR:" + e.getMessage();
        }
    }

    // ✅ Get all verifications for a user
    @GetMapping("/status")
    public List<Map<String, Object>> getUserVerifications(@RequestParam("email") String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        User user = userOpt.get();
        List<GovernmentIdVerification> list = verificationRepository.findByUserId(user.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (GovernmentIdVerification v : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("status", v.getStatus().toString());
            map.put("proofUrl", v.getProofUrl());
            map.put("verifiedOn", v.getVerifiedOn());
            map.put("placeName", v.getPlaceName());
            result.add(map);
        }
        return result;
    }
}
