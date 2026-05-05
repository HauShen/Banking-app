package com.Banking_app.controllers;

import com.Banking_app.dto.mappers.UserProfileMapper;
import com.Banking_app.dto.requestBodies.UserProfileRequestBody;
import com.Banking_app.dto.responseBodies.UserProfileResponseBody;
import com.Banking_app.dto.requestBodies.UserProfileUpdateRequestBody;
import com.Banking_app.dto.requestBodies.UserRoleUpdateRequest;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.models.UserProfile;
import com.Banking_app.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;
    @Autowired
    public UserProfileController(UserProfileService userProfileService,UserProfileMapper userProfileMapper){
        this.userProfileService = userProfileService;
        this.userProfileMapper = userProfileMapper;
    }
    @PostMapping("/create")
    public ResponseEntity<UserProfileResponseBody> register(@Valid  @RequestBody UserProfileRequestBody userProfileRequestBody){
        UserProfile createUser = userProfileService.register(
                userProfileRequestBody.getUsername(),
                userProfileRequestBody.getFullName(),
                userProfileRequestBody.getEmail(),
                userProfileRequestBody.getPassword(),
                userProfileRequestBody.getRole()
                        );
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileMapper.toResponse(createUser));
    }
    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<UserProfileResponseBody> getById(@PathVariable String id){
        UserProfile user = userProfileService.getById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(userProfileMapper.toResponse(user));
    }
    @GetMapping("/get-by-username/{username}")
    public ResponseEntity<UserProfileResponseBody> getByUsername(@PathVariable String username){
        UserProfile user = userProfileService.getByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(userProfileMapper.toResponse(user));
    }
    @PutMapping("/{id}")
    public ResponseEntity<UserProfileResponseBody> updateProfile(@PathVariable String id, @Valid @RequestBody UserProfileUpdateRequestBody userProfileUpdateRequestBody){
        UserProfile updatedUser = userProfileService.updateProfile(id, userProfileUpdateRequestBody.getFullName(), userProfileUpdateRequestBody.getEmail());
        return ResponseEntity.ok(userProfileMapper.toResponse(updatedUser));
    }
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserProfileResponseBody> updateRole(@PathVariable String id, @Valid @RequestBody UserRoleUpdateRequest userRoleUpdateRequest){
        UserProfile updatedRole = userProfileService.updateRole(id,userRoleUpdateRequest.getRole());
        return ResponseEntity.ok(userProfileMapper.toResponse(updatedRole));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){
        userProfileService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
