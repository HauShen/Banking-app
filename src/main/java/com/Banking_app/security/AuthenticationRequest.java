package com.Banking_app.security;
import java.io.Serializable;

import com.Banking_app.models.enums.UserRole;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
@Getter
@Setter
public class AuthenticationRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String fullName;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private UserRole role;
}
