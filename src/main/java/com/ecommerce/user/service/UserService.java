package com.ecommerce.user.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    
    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String EMAIL_VERIFICATION_TOPIC = "emailVerificationTopic";

    public String registerUser(User credential) {
        if (userRepository.findByEmail(credential.getEmail()).isPresent()) {
            throw new RuntimeException("User with email " + credential.getEmail() + " already exists");
        }
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        credential.setVerified(false);
        credential.setCreatedAt(System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        credential.setVerificationToken(token);
        userRepository.save(credential);

        // Send Kafka event for email verification
        try {
            String message = credential.getEmail() + "," + token + "," + credential.getUsername();
            kafkaTemplate.send(EMAIL_VERIFICATION_TOPIC, message);
        } catch (Exception e) {
            System.err.println("Warning: Could not send Kafka email verification event. Kafka might be down. " + e.getMessage());
        }

        return "Registration successful! Please check your email to verify your account.";
    }

    public String verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        
        // Return HTML response for better user experience
        return "<html>" +
               "<body style='text-align:center; font-family: Arial, sans-serif; padding-top: 50px;'>" +
               "<h1 style='color:green;'>Verification Successful!</h1>" +
               "<p>Your email has been verified successfully.</p>" +
               "<p>You can now close this window and log in to the application.</p>" +
               "</body>" +
               "</html>";
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);
        
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (updatedUser.getAddress() != null) {
            existingUser.setAddress(updatedUser.getAddress());
        }
        
        User savedUser = userRepository.save(existingUser);
        
        // Publish User Updated Event
        try {
            String userJson = objectMapper.writeValueAsString(savedUser);
            kafkaTemplate.send(USER_EVENTS_TOPIC, "USER_UPDATED", userJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        
        return savedUser;
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        kafkaTemplate.send(USER_EVENTS_TOPIC, "USER_DELETED", String.valueOf(id));
    }
}
