package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody @Valid UserRegistrationRequest request) {
        return ResponseEntity.ok(userService.registerUser(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String token) {
        return ResponseEntity.ok(userService.verifyUser(token));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getUserByFormattedId(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserByUserId(userId));
    }
}
