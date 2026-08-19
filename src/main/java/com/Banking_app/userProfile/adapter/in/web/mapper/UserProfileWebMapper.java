package com.Banking_app.userProfile.adapter.in.web.mapper;
import org.springframework.stereotype.Component;

import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

@Component
public class UserProfileWebMapper {
    public RegisterUserProfileCommand toCommand(UserProfileRequestBody body) {
        return new RegisterUserProfileCommand(
                body.getUsername(),
                body.getFullName(),
                body.getEmail(),
                body.getPassword());
    }

    public UserProfileResponseBody toResponse(UserProfile userProfile) {
        return new UserProfileResponseBody(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getFullName(),
                userProfile.getEmail(),
                userProfile.getRole().name(),
                userProfile.getCreatedAt(),
                userProfile.getUpdatedAt());
    }
}
