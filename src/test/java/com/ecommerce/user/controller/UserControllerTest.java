/*
package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserDetailResponse;
import com.ecommerce.user.dto.UserLoginRequest;
import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterUser_Success() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setFullName("Test User");
        request.setPhoneNumber("1234567890");
        request.setRole("CUSTOMER");

        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenReturn("Registration successful! Please check your email to verify your account.");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data")
                        .value("Registration successful! Please check your email to verify your account."));
    }

    @Test
    public void testValidateUser_Success() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        UserDetailResponse response = UserDetailResponse.builder()
                .userId(1L)
                .isVerified(true)
                .userType("CUSTOMER")
                .email("test@example.com")
                .build();

        when(userService.validateUserCredentials(any(UserLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.userType").value("CUSTOMER"));
    }

    @Test
    public void testRegisterUser_ValidationFailure() throws Exception {
        // Missing required fields
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername(""); // Invalid

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL_400"));
    }
}
*/
