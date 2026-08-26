package com.Banking_app.userProfile.adapter.in.web.controllers;

import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileResponseBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserProfileUpdateRequestBody;
import com.Banking_app.userProfile.adapter.in.web.dto.UserRoleUpdateRequestBody;
import com.Banking_app.userProfile.adapter.in.web.mapper.UserProfileWebMapper;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.in.command.ChangeUserRoleCommand;
import com.Banking_app.userProfile.application.port.in.command.CreateAdminCommand;
import com.Banking_app.userProfile.application.port.in.command.DeleteUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.command.UpdateUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.application.port.in.query.SortDirection;
import com.Banking_app.userProfile.application.port.in.query.UserSortField;
import com.Banking_app.userProfile.application.port.in.usecase.*;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import com.Banking_app.userProfile.common.PageResult;
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
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final ListUserProfilesUseCase listUserProfilesUseCase;
    private final DeleteUserProfileUseCase deleteUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final UserProfileWebMapper webMapper;

    public AdminUserProfileController(CreateAdminUserProfileUseCase createAdminUseCase,
                                      ChangeUserRoleUseCase changeUserRoleUseCase,
                                      GetUserProfileUseCase getUserProfileUseCase,
                                      ListUserProfilesUseCase listUserProfilesUseCase,
                                      DeleteUserProfileUseCase deleteUserProfileUseCase,
                                      UpdateUserProfileUseCase updateUserProfileUseCase,
                                      UserProfileWebMapper webMapper) {
        this.createAdminUseCase = createAdminUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.listUserProfilesUseCase = listUserProfilesUseCase;
        this.deleteUserProfileUseCase = deleteUserProfileUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
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

    @GetMapping
    public ResponseEntity<PageResult<UserProfileResponseBody>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "USERNAME") UserSortField sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection direction) {

        PageResult<UserProfile> users = listUserProfilesUseCase.list(
                new ListUserProfilesQuery(page, size, sortBy, direction));

        return ResponseEntity.ok(users.map(webMapper::toResponse));

    }
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponseBody> getById(@PathVariable String id) {
        return ResponseEntity.ok(webMapper.toResponse(getUserProfileUseCase.byId(id)));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserProfileResponseBody> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(webMapper.toResponse(getUserProfileUseCase.byUsername(username)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponseBody> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody UserProfileUpdateRequestBody body,
            @AuthenticationPrincipal SecurityUserDetails principal) {

        UserProfile updated = updateUserProfileUseCase.updateProfile(new UpdateUserProfileCommand(
                id,                         // target
                body.getFullName(),
                body.getEmail(),
                principal.getUserId()));    // actor — different person, so this IS audited

        return ResponseEntity.ok(webMapper.toResponse(updated));
    }
}

