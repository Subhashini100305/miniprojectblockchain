package com.miniproject.verificationApp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddReviewRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Review text is required")
    @Size(
            min = 15,
            max = 2000,
            message = "Review must be between 15 and 2000 characters"
    )
    private String review;

    @NotBlank(message = "Place is required")
    private String place;

    @Min(value = 1)
    @Max(value = 5)
    private Integer rating = 1;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
