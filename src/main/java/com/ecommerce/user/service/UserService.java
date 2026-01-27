gpackage com.ecommerce.user.service;

import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.UserNotVerifiedException;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String EMAIL_VERIFICATION_TOPIC = "emailVerificationTopic";

    public String registerUser(UserRegistrationRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .userId(generateFormattedId())
                .isVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .createdAt(System.currentTimeMillis())
                .build();

        repository.save(user);

        // Send Kafka event
        String message = user.getEmail() + "," + user.getVerificationToken() + "," + user.getUsername();
        kafkaTemplate.send(EMAIL_VERIFICATION_TOPIC, message);

        return "Registration successful! Please check your email to verify account.";
    }

    public String verifyUser(String token) {
        User user = repository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        
        if (user.isVerified()) {
            return "Email already verified!";
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        repository.save(user);
        return "Email verified successfully! You can now login.";
    }

    public User getUserById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.isVerified()) {
            throw new UserNotVerifiedException("User is not verified. Please verify your email first.");
        }
        
        return user;
    }

    public User getUserByUserId(String userId) {
        User user = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            throw new UserNotVerifiedException("User is not verified. Please verify your email first.");
        }

        return user;
    }

    private String generateFormattedId() {
        // Format: STZ-YYYY-XXXXX (e.g. STZ-2026-12345)
        int year = Year.now().getValue();
        int randomNum = 10000 + new Random().nextInt(90000);
        return "STZ-" + year + "-" + randomNum;
    }
}
