package com.ecommerce.user.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String EMAIL_VERIFICATION_TOPIC = "emailVerificationTopic";

    public String saveUser(User credential) {
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        credential.setVerified(false);
        credential.setCreatedAt(System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        credential.setVerificationToken(token);
        repository.save(credential);

        // Send Kafka event for email verification
        String message = credential.getEmail() + "," + token + "," + credential.getUsername();
        kafkaTemplate.send(EMAIL_VERIFICATION_TOPIC, message);

        return "Registration successful! Please check your email to verify your account.";
    }

    public String verifyUser(String token) {
        User user = repository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        user.setVerified(true);
        user.setVerificationToken(null);
        repository.save(user);
        return "Email verified successfully! You can now log in.";
    }

    public String generateToken(String username) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email before logging in.");
        }
        return jwtService.generateToken(username);
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }
}
