package com.Banking_app.userProfile.adapter.in.web.controllers;

import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileUpdateRequestBody;
import com.Banking_app.userProfile.adapter.in.web.mapper.UserProfileWebMapper;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.command.UpdateUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.RegisterUserProfileUseCase;
import com.Banking_app.userProfile.application.port.in.usecase.UpdateUserProfileUseCase;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import com.Banking_app.userProfile.application.port.in.usecase.GetUserProfileUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final RegisterUserProfileUseCase registerUserProfileUseCase;
    private final UserProfileWebMapper webMapper;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;

    public UserProfileController(RegisterUserProfileUseCase registerUserProfileUseCase,
                                 UserProfileWebMapper webMapper, GetUserProfileUseCase getUserProfileUseCase, UpdateUserProfileUseCase updateUserProfileUseCase) {
        this.registerUserProfileUseCase = registerUserProfileUseCase;
        this.webMapper = webMapper;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
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
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseBody> me(
            @AuthenticationPrincipal SecurityUserDetails principal) {

        UserProfile me = getUserProfileUseCase.byId(principal.getUserId());
        return ResponseEntity.ok(webMapper.toResponse(me));
    }
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponseBody> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequestBody body,
            @AuthenticationPrincipal SecurityUserDetails principal) {

        UserProfile updated = updateUserProfileUseCase.updateProfile(new UpdateUserProfileCommand(
                principal.getUserId(),      // target
                body.getFullName(),
                body.getEmail(),
                principal.getUserId()));    // actor — same person, so nothing is audited

        return ResponseEntity.ok(webMapper.toResponse(updated));
    }

}
