package com.miniproject.verificationApp.nlp;

public class LocationKeywordChecker {

    public boolean containsLocation(String review, String place) {

        review = review.toLowerCase();
        place = place.toLowerCase();

        // Exact full place match
        if(review.contains(place)) {
            return true;
        }

        // Split place words and check individually
        String[] placeWords = place.split("\\s+");

        int matchedWords = 0;

        for(String word : placeWords) {

            if(word.length() < 3) {
                continue;
            }

            if(review.contains(word)) {
                matchedWords++;
            }
        }

        // Require at least half the place words
        return matchedWords >= Math.max(1, placeWords.length / 2);
    }
}