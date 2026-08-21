package com.Banking_app.userProfile.adapter.in.web.controllers;

import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserRoleUpdateRequestBody;
import com.Banking_app.userProfile.adapter.in.web.mapper.UserProfileWebMapper;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.in.command.ChangeUserRoleCommand;
import com.Banking_app.userProfile.application.port.in.command.CreateAdminCommand;
import com.Banking_app.userProfile.application.port.in.command.DeleteUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.ChangeUserRoleUseCase;
import com.Banking_app.userProfile.application.port.in.usecase.CreateAdminUserProfileUseCase;
import com.Banking_app.userProfile.application.port.in.usecase.DeleteUserProfileUseCase;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserProfileController {
    private final CreateAdminUserProfileUseCase createAdminUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final DeleteUserProfileUseCase deleteUserProfileUseCase;
    private final UserProfileWebMapper webMapper;

    public AdminUserProfileController(CreateAdminUserProfileUseCase createAdminUseCase,
                                      ChangeUserRoleUseCase changeUserRoleUseCase,
                                      DeleteUserProfileUseCase deleteUserProfileUseCase,
                                      UserProfileWebMapper webMapper) {
        this.createAdminUseCase = createAdminUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
        this.deleteUserProfileUseCase = deleteUserProfileUseCase;
        this.webMapper = webMapper;
    }
    @PostMapping
    public ResponseEntity<UserProfileResponseBody> createAdmin(
            @Valid @RequestBody UserProfileRequestBody requestBody,
            @AuthenticationPrincipal SecurityUserDetails principal) {

        UserProfile created = createAdminUseCase.createAdmin(
                new CreateAdminCommand(webMapper.toCommand(requestBody), principal.getUserId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(created));
    }
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserProfileResponseBody> changeRole(
            @PathVariable String id,
            @Valid @RequestBody UserRoleUpdateRequestBody requestBody,
            @AuthenticationPrincipal SecurityUserDetails principal) {

        UserProfile updated = changeUserRoleUseCase.changeRole(
                new ChangeUserRoleCommand(id, requestBody.getRole(), principal.getUserId()));

        return ResponseEntity.ok(webMapper.toResponse(updated));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal SecurityUserDetails principal) {

        deleteUserProfileUseCase.delete(new DeleteUserProfileCommand(id, principal.getUserId()));

        return ResponseEntity.noContent().build();
    }

}
