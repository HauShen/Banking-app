package com.Banking_app.security;
import java.io.Serializable;

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
    // NOTE: deliberately no `role` field. This DTO backs the public /api/auth/register
    // endpoint, and a client must not be able to influence the role it is granted.
}
