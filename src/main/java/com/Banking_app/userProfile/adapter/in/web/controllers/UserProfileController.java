package com.Banking_app.userProfile.adapter.in.web.controllers;

import com.Banking_app.dto.mappers.UserProfileMapper;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileUpdateRequestBody;
import com.Banking_app.dto.requestBodies.UserRoleUpdateRequest;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.adapter.in.web.mapper.UserProfileWebMapper;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.RegisterUserProfileUseCase;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

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
