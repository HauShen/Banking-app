package com.Banking_app.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Body of POST /api/auth/change-password. */
@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Enter your current password")
    private String currentPassword;

    /** Same rule as registration (UserProfileRequestBody). */
    @NotBlank(message = "Enter a new password")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String newPassword;

    /** Overridden so a stray log line can never print a password. */
    @Override
    public String toString() {
        return "ChangePasswordRequest{}";
    }
}
