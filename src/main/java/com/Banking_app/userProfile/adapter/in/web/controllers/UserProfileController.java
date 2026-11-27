package com.Banking_app.userProfile.adapter.in.web.controllers;

import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileUpdateRequestBody;
import com.Banking_app.userProfile.adapter.in.web.mapper.UserProfileWebMapper;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.in.command.UpdateUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.UpdateUserProfileUseCase;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import com.Banking_app.userProfile.application.port.in.usecase.GetUserProfileUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileWebMapper webMapper;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;

    public UserProfileController(UserProfileWebMapper webMapper, GetUserProfileUseCase getUserProfileUseCase,
                                 UpdateUserProfileUseCase updateUserProfileUseCase) {
        this.webMapper = webMapper;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
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
