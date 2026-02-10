package com.ecommerce.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailResponse {
    private Long userId;
    private boolean isVerified;
    private String userType;
    private String email;
    private String accessToken;
    private String refreshToken;
}
