package com.Banking_app.controllers;
import com.Banking_app.dto.mappers.UserProfileMapper;
import com.Banking_app.dto.requestBodies.UserProfileRequestBody;
import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.dto.responseBodies.UserProfileResponseBody;
import com.Banking_app.models.UserProfile;
import com.Banking_app.service.BootstrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/auth")
public class AuthBootstrapController {
    private final BootstrapService bootstrapService;
    private final UserProfileMapper userProfileMapper;
    @Autowired
    public AuthBootstrapController(BootstrapService bootstrapService, UserProfileMapper userProfileMapper){
        this.bootstrapService = bootstrapService;
        this.userProfileMapper = userProfileMapper;
    }
    @GetMapping("/bootstrap-status")
    public ResponseEntity<BootstrapStatusResponse> getBootstrapStatus() {
        return ResponseEntity.ok(bootstrapService.getStatus());
    }
    @PostMapping("/bootstrap-admin/register")
    public ResponseEntity<UserProfileResponseBody> registerBootstrapAdmin(@Valid @RequestBody UserProfileRequestBody body) {
        UserProfile created = bootstrapService.registerFirstAdmin(
                body.getUsername(),
                body.getFullName(),
                body.getEmail(),
                body.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileMapper.toResponse(created));
    }
}
