package com.Banking_app.security;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
@Getter
public class AuthenticationResponse{
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    public AuthenticationResponse(String accessToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }
    public AuthenticationResponse(){}
}
