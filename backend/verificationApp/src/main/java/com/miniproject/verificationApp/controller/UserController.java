package com.miniproject.verificationApp.controller;

import com.miniproject.verificationApp.model.User;
import com.miniproject.verificationApp.model.EmailVerificationToken;
import com.miniproject.verificationApp.repository.UserRepository;
import com.miniproject.verificationApp.repository.EmailVerificationTokenRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ Registration
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            return "Email already registered";
        }

        // Hash password before saving
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setIsActive(true);
        user.setEmailVerified(false);
        userRepository.save(user);
        return "Registration successful! Verify your email.";
    }

    // ✅ Login (Updated to return JSON)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No account found"));
        }

        User user = userOpt.get();

        // Compare password hash
        if (!passwordEncoder.matches(loginRequest.getPasswordHash(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid password"));
        }

        if (!user.getIsActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Account is inactive"));
        }

        // ✅ JSON Response with email verification status
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful!");
        response.put("emailVerified", user.getEmailVerified());

        return ResponseEntity.ok(response);
    }

    // ✅ Send verification token
    @PostMapping("/send-token")
    public String sendToken(@RequestBody EmailRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) return "Email not registered";

        User user = userOpt.get();

        String token = UUID.randomUUID().toString();
        String tokenHash = hash(token);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(30);

        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setUser(user);
        emailToken.setTokenHash(tokenHash);
        emailToken.setExpiresAt(expiry);
        tokenRepository.save(emailToken);

        try {
            sendEmail(user.getEmail(), token);
            return "Verification token sent to your email!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send email: " + e.getMessage();
        }
    }

    // ✅ Verify token
    @PostMapping("/verify-token")
    public String verifyToken(@RequestBody VerificationRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) return "Email not registered";

        User user = userOpt.get();

        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (tokenOpt.isEmpty()) return "No token found. Request a token first.";

        EmailVerificationToken tokenEntity = tokenOpt.get();

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) return "Token expired!";
        if (!hash(request.getToken()).equals(tokenEntity.getTokenHash())) return "Invalid token!";

        user.setEmailVerified(true);
        userRepository.save(user);

        return "Email verified successfully!";
    }

    // ✅ Helper: Hash function
    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Helper: Send Email
    private void sendEmail(String toEmail, String token) throws MessagingException {
        String fromEmail = "veri10fication@gmail.com";
        String fromPassword = "swmi lvfh wwqu edyc"; // Gmail App password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, fromPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Your Verification Token");
        message.setText("Hello,\n\nYour verification token is: " + token +
                "\nThis token expires in 30 minutes.\n\nThank you.");
        Transport.send(message);
    }

    // ✅ DTOs
    public static class EmailRequest {
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class VerificationRequest {
        private String email;
        private String token;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
