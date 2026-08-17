package com.miniproject.verificationApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniproject.verificationApp.dto.AddReviewRequest;
import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.Review;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.nlp.QualityResult;
import com.miniproject.verificationApp.nlp.ReviewQualityService;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import com.miniproject.verificationApp.repository.ReviewFlagRepository;
import com.miniproject.verificationApp.repository.ReviewRepository;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.service.BlockchainService;
import com.miniproject.verificationApp.service.JwtService;
import com.miniproject.verificationApp.service.ReviewIntegrityService;
import com.miniproject.verificationApp.service.TimeDecayService;
import com.miniproject.verificationApp.service.TrustScoringService;
import com.miniproject.verificationApp.service.UserTrustScoringService;
import com.miniproject.verificationApp.service.TokenRevocationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private TimeDecayService timeDecayService;
    @MockBean private ReviewFlagRepository reviewFlagRepository;
    @MockBean private ReviewRepository reviewRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private BlockchainService blockchainService;
    @MockBean private ReviewQualityService reviewQualityService;
    @MockBean private GovernmentIdVerificationRepository verificationRepository;
    @MockBean private JwtService jwtService;
    @MockBean private TrustScoringService trustScoringService;
    @MockBean private UserTrustScoringService userTrustScoringService;
    @MockBean private TokenRevocationService tokenRevocationService;
    @MockBean private ReviewIntegrityService reviewIntegrityService;

    private User user;
    private static final String GOOD_REVIEW_TEXT =
            "This place was absolutely wonderful and well worth the visit.";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setGovernmentIdVerified(true);

        when(jwtService.extractEmail("valid-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        lenient().when(reviewIntegrityService.isBlockchainHashValid(any(Review.class)))
                .thenReturn(true);
    }

    private AddReviewRequest buildRequest(String text, String place, Integer rating) {
        AddReviewRequest request = new AddReviewRequest();
        request.setEmail("user@test.com");
        request.setReview(text);
        request.setPlace(place);
        request.setRating(rating);
        return request;
    }

    // Review.id has no public setter (only @GeneratedValue + getId()),
    // so tests that need a specific id use reflection to set it directly.
    private void setId(Review review, Long id) throws Exception {
        var field = Review.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(review, id);
    }

    // =========================
    // /add
    // =========================

    @Test
    void addReview_returnsForbidden_whenNoVerifiedProofForPlace() throws Exception {
        when(verificationRepository
                .findTopByUserIdAndPlaceNameAndStatusOrderByCreatedAtDesc(
                        1L, "Taj Mahal", GovernmentIdVerification.Status.VERIFIED))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/reviews/add")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(GOOD_REVIEW_TEXT, "Taj Mahal", 5))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_returnsRejected_whenQualityCheckFails() throws Exception {
        GovernmentIdVerification verification = new GovernmentIdVerification();
        verification.setAiConfidenceScore(80.0);
        verification.setGpsVerified(true);

        when(verificationRepository
                .findTopByUserIdAndPlaceNameAndStatusOrderByCreatedAtDesc(
                        1L, "Taj Mahal", GovernmentIdVerification.Status.VERIFIED))
                .thenReturn(Optional.of(verification));

        when(reviewQualityService.analyze(anyString(), eq("Taj Mahal")))
                .thenReturn(new QualityResult(40, false, List.of("Review too short")));

        mockMvc.perform(post("/api/reviews/add")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(GOOD_REVIEW_TEXT, "Taj Mahal", 5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.qualityScore").value(40));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void addReview_returnsAccepted_whenVerifiedAndQualityPasses() throws Exception {
        GovernmentIdVerification verification = new GovernmentIdVerification();
        verification.setAiConfidenceScore(85.0);
        verification.setGpsVerified(true);

        when(verificationRepository
                .findTopByUserIdAndPlaceNameAndStatusOrderByCreatedAtDesc(
                        1L, "Taj Mahal", GovernmentIdVerification.Status.VERIFIED))
                .thenReturn(Optional.of(verification));

        when(reviewQualityService.analyze(anyString(), eq("Taj Mahal")))
                .thenReturn(new QualityResult(90, true, Collections.emptyList()));

        when(trustScoringService.calculateTrustPoints(any(Review.class))).thenReturn(75.0);

        when(blockchainService.storeHash(anyString(), anyString(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture("0xTxHash"));

        mockMvc.perform(post("/api/reviews/add")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest(GOOD_REVIEW_TEXT, "Taj Mahal", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.aiConfidence").value(85.0))
                .andExpect(jsonPath("$.trustPoints").value(75.0));

        verify(reviewRepository, atLeastOnce()).save(any(Review.class));
    }

    // =========================
    // /flag
    // =========================

    @Test
    void flagReview_returnsBadRequest_whenUserNotGovIdVerified() throws Exception {
        user.setGovernmentIdVerified(false);

        Review review = new Review();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        mockMvc.perform(post("/api/reviews/flag")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":\"10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Only verified users can flag"));
    }

    @Test
    void flagReview_returnsBadRequest_whenAlreadyFlaggedByUser() throws Exception {
        Review review = new Review();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewFlagRepository.existsByReviewIdAndUserId(10L, 1L)).thenReturn(true);

        mockMvc.perform(post("/api/reviews/flag")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":\"10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Already flagged"));
    }

    @Test
    void flagReview_marksDisputed_whenFlagCountReachesThree() throws Exception {
        Review reviewBeforeIncrement = new Review();
        Review reviewAfterIncrement = new Review();
        reviewAfterIncrement.setFlagCount(3);

        when(reviewRepository.findById(10L))
                .thenReturn(Optional.of(reviewBeforeIncrement)) // first lookup (existence check)
                .thenReturn(Optional.of(reviewAfterIncrement));  // second lookup (after increment)
        when(reviewFlagRepository.existsByReviewIdAndUserId(10L, 1L)).thenReturn(false);
        when(reviewRepository.incrementFlagCount(10L)).thenReturn(1);
        when(trustScoringService.calculateTrustPoints(reviewAfterIncrement)).thenReturn(30.0);

        mockMvc.perform(post("/api/reviews/flag")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewId\":\"10\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Flag added successfully"));

        verify(reviewFlagRepository).saveAndFlush(any());
        assert reviewAfterIncrement.getIsDisputed();
    }

    // =========================
    // /all-with-trust  &  /search-with-trust
    // =========================

    @Test
    void getAllWithTrust_returnsReviewsSortedByFinalScoreDescending() throws Exception {
        Review low = new Review();
        setId(low, 1L);
        low.setUser(user);
        low.setTrustPoints(20.0);

        Review high = new Review();
        setId(high, 2L);
        high.setUser(user);
        high.setTrustPoints(90.0);

        when(reviewRepository.findAll()).thenReturn(List.of(low, high));
        when(userTrustScoringService.calculateUserTrustScore(user)).thenReturn(50.0);

        mockMvc.perform(get("/api/reviews/all-with-trust")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2)) // higher trustPoints sorts first
                .andExpect(jsonPath("$[1].id").value(1));
    }

    @Test
    void searchWithTrust_returnsMatchingBlockchainStoredReviews() throws Exception {
        Review review = new Review();
        setId(review, 1L);
        review.setUser(user);
        review.setPlaceName("Taj Mahal");
        review.setTrustPoints(60.0);

        when(reviewRepository
                .findByPlaceNameContainingIgnoreCaseAndStoredOnBlockchainTrue("Taj"))
                .thenReturn(List.of(review));
        when(userTrustScoringService.calculateUserTrustScore(user)).thenReturn(40.0);

        mockMvc.perform(get("/api/reviews/search-with-trust").param("place", "Taj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeName").value("Taj Mahal"));
    }

    // =========================
    // /all
    // =========================

    @Test
    void getAll_returnsPagedReviews() throws Exception {
        Review review = new Review();
        setId(review, 1L);

        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reviews/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getAll_filtersOutReviewsThatFailBlockchainIntegrityCheck() throws Exception {
        Review valid = new Review();
        setId(valid, 1L);

        Review tampered = new Review();
        setId(tampered, 2L);

        Page<Review> page = new PageImpl<>(List.of(valid, tampered));
        when(reviewRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(reviewIntegrityService.isBlockchainHashValid(valid)).thenReturn(true);
        when(reviewIntegrityService.isBlockchainHashValid(tampered)).thenReturn(false);

        mockMvc.perform(get("/api/reviews/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    // =========================
    // /my-reviews
    // =========================

    @Test
    void getMyReviews_returnsUsersBlockchainStoredReviews() throws Exception {
        Review review = new Review();
        setId(review, 1L);
        when(reviewRepository.findByUserIdAndStoredOnBlockchainTrue(1L))
                .thenReturn(List.of(review));

        mockMvc.perform(get("/api/reviews/my-reviews")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // =========================
    // /search
    // =========================

    @Test
    void searchReviews_returnsPagedResultsForPlace() throws Exception {
        Review review = new Review();
        setId(review, 1L);
        review.setPlaceName("Taj Mahal");

        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository
                .findByPlaceNameContainingIgnoreCaseAndStoredOnBlockchainTrueOrderByTrustPointsDesc(
                        eq("Taj"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reviews/search").param("place", "Taj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].placeName").value("Taj Mahal"));
    }

    // =========================
    // /rating
    // =========================

    @Test
    void getRatings_returnsRawAndDecayedRatings() throws Exception {
        Review review = new Review();
        review.setRating(4);
        review.setCreatedAt(LocalDateTime.now());

        when(reviewRepository.findByPlaceName("Taj Mahal")).thenReturn(List.of(review));
        when(timeDecayService.calculateRawRating(List.of(review))).thenReturn(4.0);
        when(timeDecayService.calculateDecayedRating(List.of(review))).thenReturn(3.8);

        mockMvc.perform(get("/api/reviews/rating").param("place", "Taj Mahal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawRating").value(4.0))
                .andExpect(jsonPath("$.decayedRating").value(3.8));
    }
}
