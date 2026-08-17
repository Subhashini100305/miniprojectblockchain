package com.miniproject.verificationApp.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

@Service
public class OCRService {

    private static final Logger logger =
            LoggerFactory.getLogger(OCRService.class);
    private static final String OCR_API_URL =
            "https://api.ocr.space/parse/image";

    @Value("${ocr.api.key}")
    private String ocrApiKey;

    public String extractText(File imageFile) {
        try {
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder()
                    .encodeToString(fileContent);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body =
                    new LinkedMultiValueMap<>();
            body.add("apikey", ocrApiKey);
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("scale", "true");
            body.add("isTable", "true");
            body.add(
                    "base64Image",
                    "data:image/jpeg;base64," + base64Image
            );

            HttpEntity<MultiValueMap<String, String>> requestEntity =
                    new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    OCR_API_URL,
                    requestEntity,
                    String.class
            );

            String jsonResponse = response.getBody();
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                logger.info("OCR completed result=EMPTY_RESPONSE");
                return "";
            }

            JSONObject json = new JSONObject(jsonResponse);
            JSONArray parsedResults = json.optJSONArray("ParsedResults");
            if (parsedResults != null && parsedResults.length() > 0) {
                String extractedText = parsedResults
                        .getJSONObject(0)
                        .optString("ParsedText", "")
                        .trim();
                logger.info("OCR completed result=TEXT_EXTRACTED");
                return extractedText;
            }

            logger.info("OCR completed result=NO_PARSED_RESULTS");
            return "";

        } catch (IOException e) {
            logger.error(
                    "OCR file read failed exceptionType={}",
                    e.getClass().getName()
            );
            return "";
        } catch (Exception e) {
            logger.error(
                    "OCR request failed exceptionType={}",
                    e.getClass().getName()
            );
            return "";
        }
    }

    public boolean isValidTravelProof(String text, String selectedPlace) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        text = text.toLowerCase();
        selectedPlace = selectedPlace.toLowerCase();

        return text.contains(selectedPlace)
                || text.contains("ticket")
                || text.contains("entry")
                || text.contains("booking")
                || text.contains("hotel")
                || text.contains("travel")
                || text.contains("tour")
                || text.contains("guide")
                || text.contains("visit");
    }
}
