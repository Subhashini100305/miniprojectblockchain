package com.miniproject.verificationApp.controller;

import com.miniproject.verificationApp.dto.LoginRequest;
import com.miniproject.verificationApp.dto.RegisterRequest;
import com.miniproject.verificationApp.dto.UserTrustScoreDTO;
import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.model.EmailVerificationToken;
import com.miniproject.verificationApp.model.GovernmentIdVerification;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.repository.EmailVerificationTokenRepository;
import com.miniproject.verificationApp.repository.GovernmentIdVerificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import com.miniproject.verificationApp.service.JwtService;
import com.miniproject.verificationApp.service.TokenRevocationService;
import com.miniproject.verificationApp.service.UserTrustScoringService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);

    private static final String TOKEN_RESPONSE =
            "If the email is registered, a verification token has been sent.";

    @Value("${gmail.email}")
    private String fromEmail;

    @Value("${gmail.password}")
    private String fromPassword;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;
    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private UserTrustScoringService userTrustScoringService;

    @Autowired
    private GovernmentIdVerificationRepository verificationRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // ✅ Registration
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request
    ) {

        Optional<User> existing =
                userRepository.findByEmail(request.getEmail());

        if (existing.isPresent()) {
            return ResponseEntity.badRequest()
                    .body("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPasswordHash()));

        user.setIsActive(true);
        user.setEmailVerified(false);

        userRepository.save(user);

        return ResponseEntity.ok(
                "Registration successful! Verify your email."
        );
    }

    // ✅ Login
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest
    ) {

        Optional<User> userOpt =
                userRepository.findByEmail(
                        loginRequest.getEmail()
                );

        if (userOpt.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid credentials"
                    ));
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(
                loginRequest.getPasswordHash(),
                user.getPasswordHash()
        )) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid credentials"
                    ));
        }

        if (!user.getIsActive()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Account is inactive"
                    ));
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Email verification required"
                    ));
        }

        Map<String, Object> response =
        new HashMap<>();

        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        response.put(
                "token",
                token
        );

        response.put(
                "message",
                "Login successful!"
        );

        response.put(
                "emailVerified",
                user.getEmailVerified()
        );

        return ResponseEntity.ok(response);
    }

    // ✅ Send verification token
    @PostMapping("/send-token")
    public String sendToken(
            @RequestBody EmailRequest request
    ) {

        Optional<User> userOpt =
                userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return TOKEN_RESPONSE;
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerificationToken> latestToken =
                tokenRepository.findTopByUserOrderByCreatedAtDesc(user);

        if (latestToken.isPresent()
                && latestToken.get().getCreatedAt()
                .isAfter(now.minusMinutes(1))) {
            return TOKEN_RESPONSE;
        }

        String token = UUID.randomUUID().toString();
        String tokenHash = hash(token);

        EmailVerificationToken emailToken =
                new EmailVerificationToken();

        emailToken.setUser(user);
        emailToken.setTokenHash(tokenHash);
        emailToken.setExpiresAt(now.plusMinutes(30));

        tokenRepository.save(emailToken);

        try {
            sendEmail(user.getEmail(), token);
        } catch (Exception e) {
            tokenRepository.delete(emailToken);
            logger.warn("Unable to send verification email");
        }

        return TOKEN_RESPONSE;
    }

    // ✅ Verify token
    @PostMapping("/verify-token")
    @Transactional
    public String verifyToken(
            @RequestBody VerificationRequest request
    ) {

        Optional<User> userOpt =
                userRepository.findByEmail(
                        request.getEmail()
                );

        if (userOpt.isEmpty()) {
            return "Email not registered";
        }

        User user = userOpt.get();

        Optional<EmailVerificationToken> tokenOpt =
                tokenRepository
                        .findTopByUserOrderByCreatedAtDesc(user);

        if (tokenOpt.isEmpty()) {
            return "No token found. Request a token first.";
        }

        EmailVerificationToken tokenEntity =
                tokenOpt.get();

        if (tokenEntity.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            return "Token expired!";
        }

        if (!hash(request.getToken())
                .equals(tokenEntity.getTokenHash())) {

            return "Invalid token!";
        }

        user.setEmailVerified(true);

        userRepository.save(user);
        tokenRepository.deleteByUser(user);

        return "Email verified successfully!";
    }

    @GetMapping("/user/trust-score")
    public ResponseEntity<UserTrustScoreDTO> getUserTrustScore(
            @RequestHeader("Authorization") String authHeader
    ) {
        String email = jwtService.extractEmail(authHeader.substring(7));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<GovernmentIdVerification> verifiedRecords =
                verificationRepository.findByUserIdAndStatus(
                        user.getId(),
                        GovernmentIdVerification.Status.VERIFIED
                );

        boolean photoVerified = !verifiedRecords.isEmpty();
        boolean gpsVerified = verifiedRecords.stream()
                .anyMatch(v -> Boolean.TRUE.equals(v.getGpsVerified()));

        double avgConfidence = verifiedRecords.stream()
                .mapToDouble(v -> v.getAiConfidenceScore() == null
                        ? 0
                        : v.getAiConfidenceScore())
                .average()
                .orElse(0);

        double score =
                userTrustScoringService.calculateUserTrustScore(user);

        return ResponseEntity.ok(new UserTrustScoreDTO(
                score,
                Boolean.TRUE.equals(user.getEmailVerified()),
                photoVerified,
                gpsVerified,
                avgConfidence
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        tokenRevocationService.revoke(authHeader.substring(7));

        return ResponseEntity.ok(Map.of(
                "message", "Logout successful"
        ));
    }

    @GetMapping("/auth/validate")
    public ResponseEntity<?> validateAuthentication(
            @RequestHeader("Authorization") String authHeader
    ) {

        String email = jwtService.extractEmail(authHeader.substring(7));

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "email", email
        ));
    }

    // ✅ Hash helper
    private String hash(String input) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    md.digest(input.getBytes());

            StringBuilder sb =
                    new StringBuilder();

            for (byte b : hash) {
                sb.append(
                        String.format("%02x", b)
                );
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 hashing is unavailable");
            throw new IllegalStateException("Hashing unavailable");
        }
    }

    // ✅ Send Email
    private void sendEmail(
            String toEmail,
            String token
    ) throws MessagingException {

        Properties props =
                new Properties();

        props.put(
                "mail.smtp.auth",
                "true"
        );

        props.put(
                "mail.smtp.starttls.enable",
                "true"
        );

        props.put(
                "mail.smtp.host",
                "smtp.gmail.com"
        );

        props.put(
                "mail.smtp.port",
                "587"
        );

        Session session =
                Session.getInstance(
                        props,
                        new Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(
                                        fromEmail,
                                        fromPassword
                                );
                            }
                        }
                );

        Message message =
                new MimeMessage(session);

        message.setFrom(
                new InternetAddress(fromEmail)
        );

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );

        message.setSubject(
                "Your Verification Token"
        );

        message.setText(
                "Hello,\n\nYour verification token is: "
                        + token
                        + "\nThis token expires in 30 minutes.\n\nThank you."
        );

        Transport.send(message);
    }

    public static class EmailRequest {

        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class VerificationRequest {

        private String email;
        private String token;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}