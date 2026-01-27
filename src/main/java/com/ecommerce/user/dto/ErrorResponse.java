package com.ecommerce.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String message;
    private String errorCode;
    private List<String> errors;
    private LocalDateTime timestamp;

    public ErrorResponse(String message, String errorCode, List<String> errors) {
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }
}
