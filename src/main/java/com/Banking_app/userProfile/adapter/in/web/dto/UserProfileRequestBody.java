package com.Banking_app.userProfile.adapter.in.web.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

import lombok.Data;

@Data
public class UserProfileRequestBody {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
    private String username;
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be <= 100 characters")
    private String fullName;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    // Optional from API, default in service if null
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;

}
