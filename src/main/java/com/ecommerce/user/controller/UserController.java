package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserLoginRequest;
import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserRole;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<String>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        String result = userService.registerUser(request);
        return ResponseEntity.ok(new com.ecommerce.user.dto.ApiResponse<>(true, 200, result, null));
    }

    @PostMapping("/validate")
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<com.ecommerce.user.dto.UserDetailResponse>> validateUser(
            @Valid @RequestBody UserLoginRequest request) {
        com.ecommerce.user.dto.UserDetailResponse response = userService.validateUserCredentials(request);
        return ResponseEntity.ok(new com.ecommerce.user.dto.ApiResponse<>(
                true,
                200,
                "User validated successfully",
                response));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
        // Returning String HTML as originally implemented for browser feedback
        return ResponseEntity.ok(userService.verifyUser(token));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<User>> updateUser(@PathVariable Long id,
            @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(new com.ecommerce.user.dto.ApiResponse<>(
                true,
                200,
                "User updated successfully",
                updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new com.ecommerce.user.dto.ApiResponse<>(
                true,
                200,
                "User deleted successfully",
                null));
    }
}
