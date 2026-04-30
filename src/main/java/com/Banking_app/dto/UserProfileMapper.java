package com.Banking_app.dto;

import com.Banking_app.models.UserProfile;
import org.springframework.stereotype.Component;
@Component
public class UserProfileMapper {
    private UserProfileMapper(){}

    public UserProfileResponseBody toResponse(UserProfile user){
        return UserProfileResponseBody.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
