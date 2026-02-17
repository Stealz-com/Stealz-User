package com.ecommerce.user.service;

import com.ecommerce.user.entity.Address;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.InvalidCredentialsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String EMAIL_VERIFICATION_TOPIC = "emailVerificationTopic";

    public String registerUser(com.ecommerce.user.dto.UserRegistrationRequest request) {
        log.info("Initiating registration for user: {}", request.getUsername());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new com.ecommerce.user.exception.UserAlreadyExistsException(
                    "User with email " + request.getEmail() + " already exists");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new com.ecommerce.user.exception.UserAlreadyExistsException(
                    "User with username " + request.getUsername() + " already exists");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password and Confirm Password do not match");
        }

        User credential = new User();
        credential.setUsername(request.getUsername());
        credential.setEmail(request.getEmail());
        credential.setPassword(passwordEncoder.encode(request.getPassword()));
        credential.setPhoneNumber(request.getPhoneNumber());

        // Handle Role
        try {
            credential.setRole(com.ecommerce.user.entity.UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role. Allowed roles: ADMIN, MERCHANT, CUSTOMER");
        }

        // Handle Full Name
        if (request.getFullName() != null) {
            String[] parts = request.getFullName().trim().split("\\s+", 2);
            credential.setFirstName(parts[0]);
            if (parts.length > 1) {
                credential.setLastName(parts[1]);
            }
        }

        credential.setVerified(false);
        credential.setCreatedAt(System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        credential.setVerificationToken(token);
        log.info("Saving user credential with verification token: {}", request.getEmail());
        userRepository.save(credential);

        // Send Kafka event for email verification
        try {
            String message = credential.getEmail() + "," + token + "," + credential.getUsername() + ","
                    + credential.getRole().name();
            kafkaTemplate.send(EMAIL_VERIFICATION_TOPIC, message);
        } catch (Exception e) {
            System.err.println(
                    "Warning: Could not send Kafka email verification event. Kafka might be down. " + e.getMessage());
        }

        return "Registration successful! Please check your email to verify your account.";
    }

    public String verifyUser(String token, String email, String usertype) {
        log.info("Verifying user: {}", email);
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        // Optional: cross-verify email
        if (!user.getEmail().equals(email)) {
            throw new RuntimeException("Email mismatch for this verification token");
        }

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
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    public User updateUser(Long id, User updatedUser) {
        log.info("Updating user profile for ID: {}", id);
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

    // Address Management
    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserId(userId);
    }

    public Address addAddress(Long userId, Address address) {
        User user = getUserById(userId);
        address.setUser(user);
        if (address.isDefault()) {
            // Unset other defaults
            List<Address> addresses = addressRepository.findByUserId(userId);
            addresses.forEach(a -> {
                if (a.isDefault()) {
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            });
        }
        return addressRepository.save(address);
    }

    public Address updateAddress(Long addressId, Address updatedAddress) {
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        existingAddress.setFullName(updatedAddress.getFullName());
        existingAddress.setAddressLine(updatedAddress.getAddressLine());
        existingAddress.setCity(updatedAddress.getCity());
        existingAddress.setState(updatedAddress.getState());
        existingAddress.setZipCode(updatedAddress.getZipCode());
        existingAddress.setPhone(updatedAddress.getPhone());

        if (updatedAddress.isDefault() && !existingAddress.isDefault()) {
            // Unset other defaults
            List<Address> addresses = addressRepository.findByUserId(existingAddress.getUser().getId());
            addresses.forEach(a -> {
                if (a.isDefault()) {
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            });
            existingAddress.setDefault(true);
        } else if (!updatedAddress.isDefault()) {
            existingAddress.setDefault(false);
        }

        return addressRepository.save(existingAddress);
    }

    public void deleteAddress(Long addressId) {
        addressRepository.deleteById(addressId);
    }

    public com.ecommerce.user.dto.UserDetailResponse validateUserCredentials(
            com.ecommerce.user.dto.UserLoginRequest request) {
        log.info("Validating credentials for user: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("User is not verified. Please verify your email first.");
        }

        return com.ecommerce.user.dto.UserDetailResponse.builder()
                .userId(user.getId())
                .isVerified(user.isVerified())
                .userType(user.getRole().name())
                .email(user.getEmail())
                .build();
    }
}
