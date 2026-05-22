package com.Banking_app;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import com.Banking_app.security.AuthService;
import com.Banking_app.security.AuthenticationRequest;
import com.Banking_app.security.AuthenticationResponse;
import com.Banking_app.security.JwtService;
import com.Banking_app.security.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private AuthenticationRequest buildRegisterRequest(String username, String email) {
        AuthenticationRequest req = new AuthenticationRequest();
        req.setUsername(username);
        req.setFullName("Test User");
        req.setEmail(email);
        req.setPassword("password123");
        return req;
    }

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    // ---------------------------------------------------------------
    // register – success
    // ---------------------------------------------------------------

    @Test
    void register_success_returnsJwtToken() {
        AuthenticationRequest req = buildRegisterRequest("john", "john@mail.com");

        when(userProfileRepository.existsByUsername("john")).thenReturn(false);
        when(userProfileRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        UserProfile savedUser = new UserProfile();
        savedUser.setId("user-1");
        savedUser.setUsername("john");
        savedUser.setRole(UserRole.CUSTOMER);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("mock.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthenticationResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("mock.jwt.token",  response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        verify(passwordEncoder).encode("password123");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void register_defaultRole_isCustomer() {
        AuthenticationRequest req = buildRegisterRequest("john", "john@mail.com");
        req.setRole(null); // no role provided

        when(userProfileRepository.existsByUsername("john")).thenReturn(false);
        when(userProfileRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        when(userProfileRepository.save(argThat(u -> u.getRole() == UserRole.CUSTOMER)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        authService.register(req);

        verify(userProfileRepository).save(argThat(u -> u.getRole() == UserRole.CUSTOMER));
    }

    // ---------------------------------------------------------------
    // register – duplicate username
    // ---------------------------------------------------------------

    @Test
    void register_duplicateUsername_throwsIllegalArgumentException() {
        AuthenticationRequest req = buildRegisterRequest("john", "john@mail.com");
        when(userProfileRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // register – duplicate email
    // ---------------------------------------------------------------

    @Test
    void register_duplicateEmail_throwsIllegalArgumentException() {
        AuthenticationRequest req = buildRegisterRequest("john", "john@mail.com");
        when(userProfileRepository.existsByUsername("john")).thenReturn(false);
        when(userProfileRepository.existsByEmail("john@mail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // login – success
    // ---------------------------------------------------------------

    @Test
    void login_validCredentials_returnsJwtToken() {
        LoginRequest req = buildLoginRequest("john", "password123");

        UserProfile user = new UserProfile();
        user.setUsername("john");
        user.setRole(UserRole.CUSTOMER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate() returns Authentication, we don't need it here
        when(userProfileRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mock.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthenticationResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // ---------------------------------------------------------------
    // login – bad credentials (AuthenticationManager throws)
    // ---------------------------------------------------------------

    @Test
    void login_badCredentials_throwsBadCredentialsException() {
        LoginRequest req = buildLoginRequest("john", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
        verify(userProfileRepository, never()).findByUsername(any());
    }

    // ---------------------------------------------------------------
    // login – user not found after authentication (edge case)
    // ---------------------------------------------------------------

    @Test
    void login_userNotFoundAfterAuth_throwsBadCredentialsException() {
        LoginRequest req = buildLoginRequest("ghost", "password123");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userProfileRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}
