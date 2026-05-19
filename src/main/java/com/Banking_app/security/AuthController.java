package com.Banking_app.security;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final UserProfileRepository userProfileRepository;
    @Autowired
    public AuthController(AuthService authService, BootstrapAdminProperties bootstrapAdminProperties, UserProfileRepository userProfileRepository){
        this.authService = authService;
        this.userProfileRepository = userProfileRepository;
        this.bootstrapAdminProperties = bootstrapAdminProperties;
    }
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    //  bootstrap status check
    @GetMapping("/bootstrap-status")
    public ResponseEntity<Map<String, Object>> bootstrapStatus() {
        boolean enabled = bootstrapAdminProperties.isEnabled();
        long adminCount = userProfileRepository.countByRole(UserRole.ADMIN);
        boolean allowed = enabled && adminCount == 0;
        return ResponseEntity.ok(Map.of(
                "bootstrapEnabled", enabled,
                "adminExists", adminCount > 0,
                "allowed", allowed
        ));
    }

    // bootstrap admin registration endpoint
    @PostMapping("/bootstrap-admin/register")
    public ResponseEntity<?> registerBootstrapAdmin(@Valid @RequestBody AuthenticationRequest request) {
        boolean enabled = bootstrapAdminProperties.isEnabled();
        long adminCount = userProfileRepository.countByRole(UserRole.ADMIN);

        if (!enabled || adminCount > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Bootstrap admin registration is not allowed"));
        }

        // Force role to ADMIN
        request.setRole(UserRole.ADMIN);
        AuthenticationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
