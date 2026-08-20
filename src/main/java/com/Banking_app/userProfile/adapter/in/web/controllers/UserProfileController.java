package com.Banking_app.userProfile.adapter.in.web.controllers;

import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.adapter.in.web.mapper.UserProfileWebMapper;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.RegisterUserProfileUseCase;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final RegisterUserProfileUseCase registerUserProfileUseCase;
    private final UserProfileWebMapper webMapper;

    public UserProfileController(RegisterUserProfileUseCase registerUserProfileUseCase,
                                 UserProfileWebMapper webMapper) {
        this.registerUserProfileUseCase = registerUserProfileUseCase;
        this.webMapper = webMapper;
    }

    @PostMapping("/create")
    public ResponseEntity<UserProfileResponseBody> register(
            @Valid @RequestBody UserProfileRequestBody requestBody) {

        RegisterUserProfileCommand command = webMapper.toCommand(requestBody);

        UserProfile registered = registerUserProfileUseCase.register(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(webMapper.toResponse(registered));
    }

}
