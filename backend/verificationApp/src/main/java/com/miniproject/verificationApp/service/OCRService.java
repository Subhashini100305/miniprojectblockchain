package com.miniproject.verificationApp.service;

import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

@Service
public class OCRService {

    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";
    private static final String OCR_API_KEY = "K82288959788957"; // Replace with valid key later

    public String extractText(File imageFile) {
        try {
            System.out.println("🔍 Sending file to OCR API (Base64 mode): " + imageFile.getAbsolutePath());

            // Convert file to Base64
            byte[] fileContent = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("apikey", OCR_API_KEY);
            body.add("language", "eng");
            body.add("isOverlayRequired", "false");
            body.add("scale", "true");
            body.add("isTable", "true");
            body.add("base64Image", "data:image/jpeg;base64," + base64Image);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OCR_API_URL, requestEntity, String.class);

            String jsonResponse = response.getBody();
            System.out.println("🧾 Raw OCR API response: " + jsonResponse);

            if (jsonResponse == null || jsonResponse.isEmpty()) return "";

            JSONObject json = new JSONObject(jsonResponse);
            JSONArray parsedResults = json.optJSONArray("ParsedResults");
            if (parsedResults != null && parsedResults.length() > 0) {
                JSONObject firstResult = parsedResults.getJSONObject(0);
                String extractedText = firstResult.optString("ParsedText", "").trim();

                System.out.println("✅ Extracted text snippet: " +
                        (extractedText.length() > 150 ? extractedText.substring(0, 150) + "..." : extractedText));

                return extractedText;
            }

            System.out.println("⚠️ No ParsedResults found in OCR response.");
            return "";

        } catch (IOException e) {
            System.err.println("❌ Error reading file: " + e.getMessage());
            e.printStackTrace();
            return "";
        } catch (Exception e) {
            System.err.println("❌ Error during OCR request: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    // ✅ Validation logic for travel proof
    public boolean isValidTravelProof(String text, String selectedPlace) {
        if (text == null || text.isEmpty()) return false;
        text = text.toLowerCase();
        selectedPlace = selectedPlace.toLowerCase();

        return text.contains(selectedPlace) ||
                text.contains("ticket") || text.contains("entry") ||
                text.contains("booking") || text.contains("hotel") ||
                text.contains("travel") || text.contains("tour") ||
                text.contains("guide") || text.contains("visit");
    }
}
