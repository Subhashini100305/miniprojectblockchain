package com.miniproject.verificationApp.service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class HashUtil {

    public static String sha256(String... fields) {
        try {
            String input = String.join("|", fields);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}