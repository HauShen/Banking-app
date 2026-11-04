package com.Banking_app.security;

import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService){
        this.authService = authService;
    }
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request,
                                                        HttpServletRequest httpRequest) {
        // getRemoteAddr(), never a client-supplied X-Forwarded-For header (anyone can fake that).
        // Behind a reverse proxy, set server.forward-headers-strategy=native so Tomcat fills this in
        // from the proxy's header, trusting only internal proxy addresses.
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    /**
     * Logs out on every device: all of this user's tokens stop working immediately (204).
     * Needs a valid token - SecurityConfig makes this the one /api/auth endpoint that isn't public.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal SecurityUserDetails principal) {
        authService.logout(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /*@GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal SecurityUserDetails principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserProfile user = principal.getUserProfile();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().name()));
    }*/

}
