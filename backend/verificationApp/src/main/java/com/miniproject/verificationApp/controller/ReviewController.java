package com.miniproject.verificationApp.controller;

import com.miniproject.verificationApp.dto.AddReviewRequest;
import com.miniproject.verificationApp.model.*;
import com.miniproject.verificationApp.repository.*;
import com.miniproject.verificationApp.service.*;
import com.miniproject.verificationApp.nlp.*;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:3000")
public class ReviewController {

    @Autowired
    private TimeDecayService timeDecayService;

    @Autowired
    private ReviewFlagRepository reviewFlagRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ReviewQualityService reviewQualityService;

    @Autowired
    private GovernmentIdVerificationRepository verificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TrustScoringService trustScoringService;

    @Autowired
    private UserTrustScoringService userTrustScoringService;

    @Autowired
    private ReviewIntegrityService reviewIntegrityService;

    // =========================
    // ADD REVIEW
    // =========================
    @PostMapping("/add")
    public ResponseEntity<?> addReview(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AddReviewRequest request
    ) {

        String email = jwtService.extractEmail(authHeader.substring(7));
        String text = request.getReview();
        String place = request.getPlace();
        Integer rating = request.getRating();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Optional<GovernmentIdVerification> verificationOpt =
                verificationRepository
                        .findTopByUserIdAndPlaceNameAndStatusOrderByCreatedAtDesc(
                                user.getId(),
                                place,
                                GovernmentIdVerification.Status.VERIFIED
                        );

        if (verificationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "REJECTED",
                    "message", "A verified proof is required for this place"
            ));
        }

        QualityResult result = reviewQualityService.analyze(text, place);

        if (!result.isAccepted()) {

            Map<String, Object> response = new HashMap<>();
            response.put("status", "REJECTED");
            response.put("qualityScore", result.getScore());
            response.put("issues", result.getIssues());

            return ResponseEntity.badRequest().body(response);
        }

        String hash = HashUtil.sha256(text, place, email);

        Review review = new Review();
        review.setUser(user);
        review.setReviewText(text);
        review.setPlaceName(place);
        review.setReviewHash(hash);
        review.setRating(rating);

        double aiConfidence = verificationOpt
                .map(GovernmentIdVerification::getAiConfidenceScore)
                .orElse(0.0);

        boolean gpsVerified =
                verificationOpt
                        .map(verification -> Boolean.TRUE.equals(verification.getGpsVerified()))
                        .orElse(false);

        review.setAiConfidenceScore(aiConfidence);

        review.setStoredOnBlockchain(false);
        review.setBlockchainTxHash("pending");

        // ✅ Added here before calculating trust points
        review.setCreatedAt(LocalDateTime.now());
        review.setTrustPoints(trustScoringService.calculateTrustPoints(review));

        reviewRepository.save(review);

        blockchainService.storeHash(
                hash,
                place,
                (int)Math.round(aiConfidence),
                result.getScore(),
                gpsVerified
        ).thenAccept(txHash -> {
            if (txHash != null) {
                review.setStoredOnBlockchain(true);
                review.setBlockchainTxHash(txHash);
                reviewRepository.save(review);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ACCEPTED");
        response.put("qualityScore", result.getScore());
        response.put("aiConfidence", aiConfidence);
        response.put("trustPoints", review.getTrustPoints());
        response.put("txHash", "pending");
        response.put("message", "Review accepted; blockchain confirmation pending.");

        return ResponseEntity.ok(response);
    }

    // =========================
    // FLAG REVIEW
    // =========================
    @PostMapping("/flag")
    @Transactional
    public ResponseEntity<?> flagReview(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request
    ) {

        Long reviewId = Long.parseLong(request.get("reviewId"));
        String email = jwtService.extractEmail(authHeader.substring(7));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (user.getGovernmentIdVerified() == null
                || !user.getGovernmentIdVerified()) {
            return ResponseEntity.badRequest()
                    .body("Only verified users can flag");
        }

        if (reviewFlagRepository
                .existsByReviewIdAndUserId(reviewId, user.getId())) {
            return ResponseEntity.badRequest().body("Already flagged");
        }

        reviewFlagRepository.saveAndFlush(
                new ReviewFlag(reviewId, user.getId())
        );

        int updatedRows = reviewRepository.incrementFlagCount(reviewId);
        if (updatedRows != 1) {
            throw new IllegalStateException("Review flag update failed");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (review.getFlagCount() >= 3) {
            review.setIsDisputed(true);
        }

        review.setTrustPoints(
                trustScoringService.calculateTrustPoints(review)
        );
        reviewRepository.save(review);

        return ResponseEntity.ok("Flag added successfully");
    }

    @GetMapping("/all-with-trust")
    public List<Map<String, Object>> getAllWithTrust() {
        List<Review> reviews = reviewRepository.findAll()
                .stream()
                .filter(reviewIntegrityService::isBlockchainHashValid)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Review review : reviews) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", review.getId());
            map.put("placeName", review.getPlaceName());
            map.put("reviewText", review.getReviewText());
            map.put("rating", review.getRating());
            map.put("trustPoints", review.getTrustPoints());
            map.put("isDisputed", review.getIsDisputed());
            map.put("storedOnBlockchain", review.getStoredOnBlockchain());
            map.put("blockchainTxHash", review.getBlockchainTxHash());
            map.put("createdAt", review.getCreatedAt());
            map.put("flagCount", review.getFlagCount());
            map.put("aiConfidenceScore", review.getAiConfidenceScore());

            double userTrust =
                    userTrustScoringService.calculateUserTrustScore(
                            review.getUser()
                    );

            double finalScore =
                    (userTrust * 0.4)
                            + (review.getTrustPoints() * 0.6);

            double cappedFinalScore = Math.min(finalScore, 100);

            map.put("userTrustScore", userTrust);
            map.put("finalScore", cappedFinalScore);
            result.add(map);
        }

        result.sort((a, b) -> Double.compare(
                (Double) b.get("finalScore"),
                (Double) a.get("finalScore")
        ));

        return result;
    }

    @GetMapping("/search-with-trust")
    public List<Map<String, Object>> searchWithTrust(
            @RequestParam String place
    ) {
        List<Review> reviews = reviewRepository
                .findByPlaceNameContainingIgnoreCaseAndStoredOnBlockchainTrue(
                        place
                )
                .stream()
                .filter(reviewIntegrityService::isBlockchainHashValid)
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Review review : reviews) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", review.getId());
            map.put("placeName", review.getPlaceName());
            map.put("reviewText", review.getReviewText());
            map.put("rating", review.getRating());
            map.put("trustPoints", review.getTrustPoints());
            map.put("isDisputed", review.getIsDisputed());
            map.put("storedOnBlockchain", review.getStoredOnBlockchain());
            map.put("blockchainTxHash", review.getBlockchainTxHash());
            map.put("createdAt", review.getCreatedAt());
            map.put("flagCount", review.getFlagCount());
            map.put("aiConfidenceScore", review.getAiConfidenceScore());

            double userTrust =
                    userTrustScoringService.calculateUserTrustScore(
                            review.getUser()
                    );

            double finalScore =
                    (userTrust * 0.4)
                            + (review.getTrustPoints() * 0.6);

            double cappedFinalScore = Math.min(finalScore, 100);

            map.put("userTrustScore", userTrust);
            map.put("finalScore", cappedFinalScore);
            result.add(map);
        }

        result.sort((a, b) -> Double.compare(
                (Double) b.get("finalScore"),
                (Double) a.get("finalScore")
        ));

        return result;
    }

    // =========================
    // GET ALL REVIEWS
    // =========================
    @GetMapping("/all")
    public Page<Review> getAll(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<Review> reviews = reviewRepository.findAll(pageable);
        List<Review> verifiedReviews = reviews.getContent()
                .stream()
                .filter(reviewIntegrityService::isBlockchainHashValid)
                .toList();

        return new PageImpl<>(
                verifiedReviews,
                pageable,
                verifiedReviews.size()
        );
    }

    // =========================
    // GET MY REVIEWS
    // =========================
    @GetMapping("/my-reviews")
    public List<Review> getMyReviews(
            @RequestHeader("Authorization") String authHeader
    ) {

        String email = jwtService.extractEmail(authHeader.substring(7));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return reviewRepository.findByUserIdAndStoredOnBlockchainTrue(user.getId())
                .stream()
                .filter(reviewIntegrityService::isBlockchainHashValid)
                .toList();
    }

    // =========================
    // SEARCH REVIEWS BY PLACE
    // =========================
    @GetMapping("/search")
    public Page<Review> searchReviews(
            @RequestParam String place,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<Review> reviews = reviewRepository
                .findByPlaceNameContainingIgnoreCaseAndStoredOnBlockchainTrueOrderByTrustPointsDesc(
                        place,
                        pageable
                );

        List<Review> verifiedReviews = reviews.getContent()
                .stream()
                .filter(reviewIntegrityService::isBlockchainHashValid)
                .toList();

        return new PageImpl<>(
                verifiedReviews,
                pageable,
                verifiedReviews.size()
        );
    }

    // =========================
    // RATINGS
    // =========================
    @GetMapping("/rating")
    public Map<String, Double> getRatings(@RequestParam String place) {

        List<Review> reviews = reviewRepository.findByPlaceName(place)
                .stream()
                .filter(reviewIntegrityService::isBlockchainHashValid)
                .toList();

        double rawRating = timeDecayService.calculateRawRating(reviews);
        double decayedRating = timeDecayService.calculateDecayedRating(reviews);

        Map<String, Double> response = new HashMap<>();
        response.put("rawRating", rawRating);
        response.put("decayedRating", decayedRating);

        return response;
    }
}
