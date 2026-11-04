package com.Banking_app.security;

import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin incident response: log a user out of every device, e.g. when their account may be compromised.
 * Lives next to the other token code; the URL sits under /api/admin/users like the other user admin actions.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTokenController {

    private final AuthService authService;
    private final LoadUserProfilePort loadUserProfilePort;

    public AdminTokenController(AuthService authService, LoadUserProfilePort loadUserProfilePort) {
        this.authService = authService;
        this.loadUserProfilePort = loadUserProfilePort;
    }

    @PostMapping("/{id}/revoke-tokens")
    public ResponseEntity<Void> revokeTokens(@PathVariable String id) {
        loadUserProfilePort.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        authService.endAllSessions(id);   // access tokens and refresh tokens
        return ResponseEntity.noContent().build();
    }
}
