package com.miniproject.verificationApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniproject.verificationApp.dto.LoginRequest;
import com.miniproject.verificationApp.dto.RegisterRequest;
import com.miniproject.verificationApp.model.EmailVerificationToken;
import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.repository.EmailVerificationTokenRepository;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.service.JwtService;
import com.miniproject.verificationApp.service.TokenRevocationService;
import com.miniproject.verificationApp.service.UserTrustScoringService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // skip JwtFilter/RateLimitFilter, this is a unit test of the controller
@TestPropertySource(properties = {
        "gmail.email=test@example.com",
        "gmail.password=testpassword"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserRepository userRepository;
    @MockBean private EmailVerificationTokenRepository tokenRepository;
    @MockBean private JwtService jwtService;
    @MockBean private TokenRevocationService tokenRevocationService;
    @MockBean private UserTrustScoringService userTrustScoringService;
    @MockBean private GovernmentIdVerificationRepository verificationRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User verifiedActiveUser;

    @BeforeEach
    void setUp() {
        verifiedActiveUser = new User();
        verifiedActiveUser.setId(1L);
        verifiedActiveUser.setName("Test User");
        verifiedActiveUser.setEmail("user@test.com");
        verifiedActiveUser.setPasswordHash(encoder.encode("correctPassword"));
        verifiedActiveUser.setIsActive(true);
        verifiedActiveUser.setEmailVerified(true);
        verifiedActiveUser.setGovernmentIdVerified(false);
    }

    // =========================
    // /register
    // =========================

    @Test
    void register_savesNewUser_whenEmailNotTaken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@test.com");
        request.setPasswordHash("plainPassword");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Registration successful! Verify your email."));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_returnsBadRequest_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Dup User");
        request.setEmail("user@test.com");
        request.setPasswordHash("plainPassword");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already registered"));

        verify(userRepository, never()).save(any(User.class));
    }

    // =========================
    // /login
    // =========================

    @Test
    void login_returnsToken_whenCredentialsValidAndVerified() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPasswordHash("correctPassword");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(jwtService.generateToken("user@test.com")).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void login_returnsUnauthorized_whenUserNotFound() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@test.com");
        request.setPasswordHash("whatever");

        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_returnsUnauthorized_whenPasswordWrong() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPasswordHash("wrongPassword");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_returnsUnauthorized_whenAccountInactive() throws Exception {
        verifiedActiveUser.setIsActive(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPasswordHash("correctPassword");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is inactive"));
    }

    @Test
    void login_returnsForbidden_whenEmailNotVerified() throws Exception {
        verifiedActiveUser.setEmailVerified(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPasswordHash("correctPassword");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Email verification required"));
    }

    // =========================
    // /send-token
    // =========================

    @Test
    void sendToken_returnsGenericMessage_whenEmailUnknown() throws Exception {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/send-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "If the email is registered, a verification token has been sent."));

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void sendToken_returnsGenericMessage_whenTokenRecentlySent() throws Exception {
        EmailVerificationToken recentToken = new EmailVerificationToken();
        recentToken.setCreatedAt(LocalDateTime.now().minusSeconds(10));

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(verifiedActiveUser))
                .thenReturn(Optional.of(recentToken));

        mockMvc.perform(post("/api/send-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "If the email is registered, a verification token has been sent."));

        // rate-limited by the 1-minute window, so no new token should be created
        verify(tokenRepository, never()).save(any());
    }

    // NOTE: The "successful token creation" branch calls the controller's private
    // sendEmail() method, which opens a real SMTP connection to smtp.gmail.com.
    // That is not something we can unit test for free/offline without either
    // (a) real network access + real Gmail app-password creds, or
    // (b) refactoring sendEmail() into an injectable EmailService interface that
    //     can be @MockBean'd like the other dependencies.
    // Recommendation: extract email sending into its own @Service (e.g. EmailService)
    // so this branch becomes trivially testable. Leaving this disabled for now.
    @Disabled("Requires refactoring sendEmail() into an injectable service to avoid real SMTP calls")
    @Test
    void sendToken_createsTokenAndSendsEmail_whenNoRecentToken() {
        // intentionally left unimplemented until sendEmail() is extracted into a service
    }

    // =========================
    // /verify-token
    // =========================

    @Test
    void verifyToken_returnsEmailNotRegistered_whenUserMissing() throws Exception {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@test.com\",\"token\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email not registered"));
    }

    @Test
    void verifyToken_returnsNoTokenFound_whenNoTokenExists() throws Exception {
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(verifiedActiveUser))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"token\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("No token found. Request a token first."));
    }

    @Test
    void verifyToken_returnsExpired_whenTokenExpired() throws Exception {
        EmailVerificationToken expiredToken = new EmailVerificationToken();
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expiredToken.setTokenHash("irrelevant");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(verifiedActiveUser))
                .thenReturn(Optional.of(expiredToken));

        mockMvc.perform(post("/api/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"token\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Token expired!"));
    }

    @Test
    void verifyToken_returnsInvalid_whenTokenMismatch() throws Exception {
        EmailVerificationToken validToken = new EmailVerificationToken();
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setTokenHash("some-other-hash-entirely");

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(verifiedActiveUser))
                .thenReturn(Optional.of(validToken));

        mockMvc.perform(post("/api/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"token\":\"wrong-token\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid token!"));
    }

    @Test
    void verifyToken_returnsSuccess_whenTokenValid() throws Exception {
        // SHA-256 of "correct-token" — controller hashes the incoming token with SHA-256
        // and compares to tokenHash, so we precompute it the same way here.
        String rawToken = "correct-token";
        String hash = sha256(rawToken);

        EmailVerificationToken validToken = new EmailVerificationToken();
        validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        validToken.setTokenHash(hash);

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(tokenRepository.findTopByUserOrderByCreatedAtDesc(verifiedActiveUser))
                .thenReturn(Optional.of(validToken));

        mockMvc.perform(post("/api/verify-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email verified successfully!"));

        verify(userRepository).save(verifiedActiveUser);
        verify(tokenRepository).deleteByUser(verifiedActiveUser);
    }

    private String sha256(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // =========================
    // /user/trust-score
    // =========================

    @Test
    void getUserTrustScore_returnsScore() throws Exception {
        when(jwtService.extractEmail("valid-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(verifiedActiveUser));
        when(verificationRepository.findByUserIdAndStatus(
                1L, GovernmentIdVerification.Status.VERIFIED))
                .thenReturn(Collections.emptyList());
        when(userTrustScoringService.calculateUserTrustScore(verifiedActiveUser))
                .thenReturn(42.5);

        mockMvc.perform(get("/api/user/trust-score")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userTrustScore").value(42.5))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.photoVerified").value(false));
    }

    // =========================
    // /logout
    // =========================

    @Test
    void logout_returnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(tokenRevocationService).revoke("some-token");
    }

    // =========================
    // /auth/validate
    // =========================

    @Test
    void validateAuthentication_returnsAuthenticatedTrue() throws Exception {
        when(jwtService.extractEmail("some-token")).thenReturn("user@test.com");

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }
}