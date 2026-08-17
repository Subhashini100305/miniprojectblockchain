package com.miniproject.verificationApp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
public class AIVerificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(AIVerificationService.class);

    // =========================================================
    // GOOGLE VISION
    // =========================================================

    @Value("${google.credentials.path:}")
    private String googleCredentialsPath;

    private ImageAnnotatorClient googleClient;

    // =========================================================
    // AZURE COMPUTER VISION 3.2
    // =========================================================

    @Value("${azure.vision.endpoint:}")
    private String azureEndpoint;

    @Value("${azure.vision.key:}")
    private String azureKey;

    private WebClient azureWebClient;

    // Jackson is already provided by Spring Boot
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================
    // INITIALIZATION
    // =========================================================

    @PostConstruct
    void initializeClients() {

        initializeGoogle();

        initializeAzure();

        if (googleClient == null && azureWebClient == null) {
            logger.error(
                    "Neither Google Vision nor Azure Vision could be initialized"
            );
        }
    }

    // =========================================================
    // GOOGLE INITIALIZATION
    // =========================================================

    private void initializeGoogle() {

        if (googleCredentialsPath == null
                || googleCredentialsPath.isBlank()) {

            logger.warn(
                    "Google Vision credentials path is not configured"
            );

            return;
        }

        File credentialsFile =
                new File(googleCredentialsPath);

        if (!credentialsFile.exists()) {

            logger.warn(
                    "Google Vision credentials file does not exist: {}",
                    googleCredentialsPath
            );

            return;
        }

        try (FileInputStream credentialsStream =
                     new FileInputStream(credentialsFile)) {

            GoogleCredentials credentials =
                    GoogleCredentials
                            .fromStream(credentialsStream)
                            .createScoped(
                                    List.of(
                                            "https://www.googleapis.com/auth/cloud-platform"
                                    )
                            );

            ImageAnnotatorSettings settings =
                    ImageAnnotatorSettings.newBuilder()
                            .setCredentialsProvider(
                                    () -> credentials
                            )
                            .build();

            googleClient =
                    ImageAnnotatorClient.create(settings);

            logger.info(
                    "Google Vision initialized successfully"
            );

        } catch (Exception e) {

            googleClient = null;

            logger.warn(
                    "Google Vision initialization failed. "
                            + "Azure will be available as fallback. exceptionType={}",
                    e.getClass().getName()
            );
        }
    }

    // =========================================================
    // AZURE INITIALIZATION
    // =========================================================

    private void initializeAzure() {

        if (azureEndpoint == null
                || azureEndpoint.isBlank()
                || azureKey == null
                || azureKey.isBlank()) {

            logger.warn(
                    "Azure Vision credentials are not configured"
            );

            return;
        }

        try {

            String cleanEndpoint =
                    azureEndpoint.endsWith("/")
                            ? azureEndpoint.substring(
                                    0,
                                    azureEndpoint.length() - 1
                            )
                            : azureEndpoint;

            azureWebClient =
                    WebClient.builder()
                            .baseUrl(cleanEndpoint)
                            .defaultHeader(
                                    "Ocp-Apim-Subscription-Key",
                                    azureKey
                            )
                            .build();

            logger.info(
                    "Azure Computer Vision 3.2 fallback initialized successfully"
            );

        } catch (Exception e) {

            azureWebClient = null;

            logger.warn(
                    "Azure Vision initialization failed. exceptionType={}",
                    e.getClass().getName()
            );
        }
    }

    // =========================================================
    // MAIN VERIFICATION METHOD
    //
    // IMPORTANT:
    //
    // Google is ALWAYS tried first.
    //
    // Azure is used ONLY if Google has a TECHNICAL/API FAILURE.
    //
    // Azure is NOT used when Google successfully processes the
    // image but rejects it or finds no landmark/text.
    // =========================================================

    public AIVerificationResult verifyProof(
            File imageFile,
            String claimedPlace
    ) {

        // =====================================================
        // 1. GOOGLE FIRST
        // =====================================================

        if (googleClient != null) {

            try {

                logger.info(
                        "Attempting AI verification using Google Vision"
                );

                return verifyUsingGoogle(
                        imageFile,
                        claimedPlace
                );

            } catch (Exception e) {

                /*
                 * THIS IS THE ONLY CONDITION THAT ACTIVATES AZURE.
                 *
                 * Examples:
                 *
                 * - Google API unavailable
                 * - network failure
                 * - credentials/API failure
                 * - Google Vision request exception
                 * - Google API returned an error
                 */

                logger.warn(
                        "Google Vision technical failure. "
                                + "Switching to Azure Vision 3.2 fallback. exceptionType={}",
                        e.getClass().getName()
                );
            }

        } else {

            logger.warn(
                    "Google Vision is unavailable. "
                            + "Using Azure Vision 3.2 fallback."
            );
        }

        // =====================================================
        // 2. AZURE FALLBACK
        // =====================================================

        if (azureWebClient != null) {

            try {

                logger.info(
                        "Attempting AI verification using Azure Computer Vision 3.2"
                );

                return verifyUsingAzure(
                        imageFile,
                        claimedPlace
                );

            } catch (Exception e) {

                logger.error(
                        "Azure Vision fallback also failed. exceptionType={}",
                        e.getClass().getName()
                );

                return AIVerificationResult.rejected(
                        "Both Google Vision and Azure Vision are unavailable",
                        "AZURE_VISION"
                );
            }
        }

        // =====================================================
        // 3. NOTHING AVAILABLE
        // =====================================================

        return AIVerificationResult.rejected(
                "AI verification services are unavailable",
                "NONE"
        );
    }

    // =========================================================
    // GOOGLE VERIFICATION
    // =========================================================

    private AIVerificationResult verifyUsingGoogle(
            File imageFile,
            String claimedPlace
    ) throws IOException {

        AnnotateImageResponse response =
                analyzeImageWithGoogle(imageFile);

        /*
         * A Google API-level error is a technical failure.
         *
         * Throwing here causes verifyProof() to activate Azure.
         */

        if (response.hasError()) {

            throw new IOException(
                    "Google Vision API error: "
                            + response.getError().getMessage()
            );
        }

        // =====================================================
        // GOOGLE LANDMARK DETECTION
        // =====================================================

        if (!response.getLandmarkAnnotationsList().isEmpty()) {

            return verifyGooglePhoto(
                    response,
                    claimedPlace
            );
        }

        // =====================================================
        // GOOGLE DOCUMENT / TEXT DETECTION
        // =====================================================

        if (!response.getTextAnnotationsList().isEmpty()) {

            return verifyGoogleDocument(
                    response,
                    claimedPlace
            );
        }

        /*
         * VERY IMPORTANT:
         *
         * Google successfully responded.
         *
         * It simply found no usable landmark/text.
         *
         * Therefore Azure is NOT called.
         */

        return AIVerificationResult.rejected(
                "No usable content found in image",
                "GOOGLE_VISION"
        );
    }

    // =========================================================
    // GOOGLE IMAGE ANALYSIS
    // =========================================================

    private AnnotateImageResponse analyzeImageWithGoogle(
            File imageFile
    ) throws IOException {

        byte[] imageBytes =
                Files.readAllBytes(
                        imageFile.toPath()
                );

        ByteString imgBytes =
                ByteString.copyFrom(imageBytes);

        Image img =
                Image.newBuilder()
                        .setContent(imgBytes)
                        .build();

        Feature landmark =
                Feature.newBuilder()
                        .setType(
                                Feature.Type.LANDMARK_DETECTION
                        )
                        .build();

        Feature text =
                Feature.newBuilder()
                        .setType(
                                Feature.Type.TEXT_DETECTION
                        )
                        .build();

        Feature face =
                Feature.newBuilder()
                        .setType(
                                Feature.Type.FACE_DETECTION
                        )
                        .build();

        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder()
                        .setImage(img)
                        .addFeatures(landmark)
                        .addFeatures(text)
                        .addFeatures(face)
                        .build();

        BatchAnnotateImagesResponse batch =
                googleClient.batchAnnotateImages(
                        List.of(request)
                );

        if (batch.getResponsesCount() == 0) {

            throw new IOException(
                    "Google Vision returned no response"
            );
        }

        return batch.getResponses(0);
    }

    // =========================================================
    // GOOGLE PHOTO VERIFICATION
    // =========================================================

    private AIVerificationResult verifyGooglePhoto(
            AnnotateImageResponse response,
            String claimedPlace
    ) {

        String detectedLandmark =
                response
                        .getLandmarkAnnotationsList()
                        .get(0)
                        .getDescription();

        float confidence =
                response
                        .getLandmarkAnnotationsList()
                        .get(0)
                        .getScore();

        if (!isMatch(
                detectedLandmark,
                claimedPlace
        )) {

            /*
             * Google successfully recognized a landmark,
             * but it does NOT match the claimed place.
             *
             * DO NOT use Azure here.
             */

            return AIVerificationResult.rejected(
                    "Detected: "
                            + detectedLandmark
                            + " but expected: "
                            + claimedPlace,
                    "GOOGLE_VISION"
            );
        }

        return AIVerificationResult.verified(
                "Photo verified at "
                        + detectedLandmark,
                confidence * 100,
                "PHOTO",
                detectedLandmark,
                "GOOGLE_VISION"
        );
    }

    // =========================================================
    // GOOGLE DOCUMENT VERIFICATION
    // =========================================================

    private AIVerificationResult verifyGoogleDocument(
            AnnotateImageResponse response,
            String claimedPlace
    ) {

        String text =
                response
                        .getTextAnnotationsList()
                        .get(0)
                        .getDescription();

        if (!isMatch(
                text,
                claimedPlace
        )) {

            /*
             * Google successfully performed OCR,
             * but the document does not contain the claimed place.
             *
             * DO NOT use Azure.
             */

            return AIVerificationResult.rejected(
                    "Place not found in document",
                    "GOOGLE_VISION"
            );
        }

        return AIVerificationResult.verified(
                "Document verified for "
                        + claimedPlace,
                80f,
                "DOCUMENT",
                claimedPlace,
                "GOOGLE_VISION"
        );
    }

    // =========================================================
    // AZURE COMPUTER VISION 3.2
    //
    // Uses the older Computer Vision 3.2 REST API because
    // this version supports LANDMARKS.
    //
    // =========================================================

    private AIVerificationResult verifyUsingAzure(
            File imageFile,
            String claimedPlace
    ) throws Exception {

        byte[] imageBytes =
                Files.readAllBytes(
                        imageFile.toPath()
                );

        /*
         * Azure Computer Vision 3.2 Analyze endpoint.
         *
         * visualFeatures:
         * - Description
         * - Tags
         *
         * details:
         * - Landmarks
         */

        String responseBody =
                azureWebClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path(
                                                "/vision/v3.2/analyze"
                                        )
                                        .queryParam(
                                                "visualFeatures",
                                                "Description,Tags"
                                        )
                                        .queryParam(
                                                "details",
                                                "Landmarks"
                                        )
                                        .queryParam(
                                                "language",
                                                "en"
                                        )
                                        .build()
                        )
                        .contentType(
                                MediaType.APPLICATION_OCTET_STREAM
                        )
                        .bodyValue(imageBytes)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

        if (responseBody == null
                || responseBody.isBlank()) {

            throw new IOException(
                    "Azure returned an empty response"
            );
        }

        JsonNode root =
                objectMapper.readTree(responseBody);

        // =====================================================
        // CHECK AZURE API ERROR
        // =====================================================

        if (root.has("error")) {

            String errorMessage =
                    root
                            .path("error")
                            .path("message")
                            .asText(
                                    "Azure Computer Vision error"
                            );

            throw new IOException(
                    errorMessage
            );
        }

        // =====================================================
        // LANDMARK DETECTION
        // =====================================================

        JsonNode categories =
                root.path("categories");

        String detectedLandmark = null;

        double landmarkConfidence = 0.0;

        if (categories.isArray()) {

            for (JsonNode category : categories) {

                JsonNode detail =
                        category.path("detail");

                JsonNode landmarks =
                        detail.path("landmarks");

                if (landmarks.isArray()
                        && !landmarks.isEmpty()) {

                    JsonNode landmark =
                            landmarks.get(0);

                    detectedLandmark =
                            landmark
                                    .path("name")
                                    .asText(null);

                    landmarkConfidence =
                            landmark
                                    .path("confidence")
                                    .asDouble(0.0);

                    break;
                }
            }
        }

        // =====================================================
        // AZURE LANDMARK FOUND
        // =====================================================

        if (detectedLandmark != null
                && !detectedLandmark.isBlank()) {

            if (!isMatch(
                    detectedLandmark,
                    claimedPlace
            )) {

                /*
                 * Azure successfully recognized a landmark,
                 * but it does not match the selected place.
                 */

                return AIVerificationResult.rejected(
                        "Detected: "
                                + detectedLandmark
                                + " but expected: "
                                + claimedPlace,
                        "AZURE_VISION"
                );
            }

            return AIVerificationResult.verified(
                    "Photo verified at "
                            + detectedLandmark
                            + " using Azure Vision",
                    (float)
                            (landmarkConfidence * 100),
                    "PHOTO",
                    detectedLandmark,
                    "AZURE_VISION"
            );
        }

        // =====================================================
        // AZURE DESCRIPTION / TAGS
        // =====================================================

        /*
         * This is NOT treated as equivalent to landmark
         * detection.
         *
         * It is only a secondary fallback interpretation.
         */

        String description = "";

        JsonNode descriptionNode =
                root.path("description");

        if (descriptionNode.has("captions")
                && descriptionNode
                .path("captions")
                .isArray()
                && !descriptionNode
                .path("captions")
                .isEmpty()) {

            description =
                    descriptionNode
                            .path("captions")
                            .get(0)
                            .path("text")
                            .asText("");
        }

        StringBuilder tags =
                new StringBuilder();

        JsonNode tagsNode =
                root.path("tags");

        if (tagsNode.isArray()) {

            for (JsonNode tag : tagsNode) {

                String tagName =
                        tag.path("name")
                                .asText("");

                if (!tagName.isBlank()) {

                    tags.append(" ")
                            .append(tagName);
                }
            }
        }

        String combinedVisualText =
                normalize(
                        description
                                + " "
                                + tags
                );

        if (isMatch(
                combinedVisualText,
                claimedPlace
        )) {

            return AIVerificationResult.verified(
                    "Photo matched using Azure Vision",
                    70f,
                    "PHOTO",
                    claimedPlace,
                    "AZURE_VISION"
            );
        }

        // =====================================================
        // AZURE COULD NOT VERIFY
        // =====================================================

        return AIVerificationResult.rejected(
                "Azure Vision could not verify the claimed place",
                "AZURE_VISION"
        );
    }

    // =========================================================
    // MATCHING
    // =========================================================

    private boolean isMatch(
            String detected,
            String claimed
    ) {

        if (detected == null
                || claimed == null
                || detected.isBlank()
                || claimed.isBlank()) {

            return false;
        }

        detected =
                normalize(detected);

        claimed =
                normalize(claimed);

        return detected.contains(claimed)
                || claimed.contains(detected);
    }

    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll(
                        "\\(.*?\\)",
                        ""
                )
                .replaceAll(
                        "[^a-z0-9 ]",
                        ""
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @PreDestroy
    void closeClients() {

        if (googleClient != null) {

            try {

                googleClient.close();

            } catch (Exception e) {

                logger.warn(
                        "Unable to close Google Vision client"
                );
            }
        }
    }
}

