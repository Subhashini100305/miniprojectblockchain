package com.miniproject.verificationApp.nlp;

import java.util.HashSet;
import java.util.Set;

public class SpamDetector {

    public boolean isSpam(String review) {

        String lower = review.toLowerCase();

        // =========================
        // REPEATED WORDS
        // =========================

        if(lower.matches(".*(\\b\\w+\\b)(\\s+\\1\\b){2,}.*")) {
            return true;
        }

        // =========================
        // TOO MANY CAPITALS
        // =========================

        long upperCount = review.chars()
                .filter(Character::isUpperCase)
                .count();

        if(review.length() > 0 &&
                (upperCount * 100 / review.length()) > 40) {
            return true;
        }

        // =========================
        // EXCESSIVE PUNCTUATION
        // =========================

        if(review.matches(".*[!?.]{4,}.*")) {
            return true;
        }

        // =========================
        // TOO MANY DUPLICATE WORDS
        // =========================

        String[] words = lower.split("\\s+");

        Set<String> uniqueWords = new HashSet<>();

        for(String word : words) {
            uniqueWords.add(word);
        }

        if(words.length > 0 &&
                uniqueWords.size() < words.length / 2) {
            return true;
        }

        return false;
    }
}