package com.Banking_app;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.repositories.UserProfileRepository;
import com.Banking_app.serviceImpl.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserProfileServiceImplTest {
    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    // ---------------------------------------------------------------
    // register – duplicate username
    // ---------------------------------------------------------------

    @Test
    void register_duplicateUsername_throwsIllegalArgumentException() {
        when(userProfileRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.register("john", "John Doe", "john@mail.com", "pass"));
        verify(userProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // register – duplicate email
    // ---------------------------------------------------------------

    @Test
    void register_duplicateEmail_throwsIllegalArgumentException() {
        when(userProfileRepository.existsByUsername("john")).thenReturn(false);
        when(userProfileRepository.existsByEmail("john@mail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.register("john", "John Doe", "john@mail.com", "pass"));
        verify(userProfileRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // registerAsAdmin – success
    // ---------------------------------------------------------------

    @Test
    void registerAsAdmin_success() {
        when(userProfileRepository.existsByUsername("admin")).thenReturn(false);
        when(userProfileRepository.existsByEmail("admin@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("adminPass")).thenReturn("encodedAdminPass");

        UserProfile saved = new UserProfile();
        saved.setId("admin-id");
        saved.setUsername("admin");
        saved.setRole(UserRole.ADMIN);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(saved);

        UserProfile result = userProfileService.registerAsAdmin("admin", "Admin User", "admin@mail.com", "adminPass");

        assertEquals(UserRole.ADMIN, result.getRole());
        verify(passwordEncoder).encode("adminPass");
    }

    // ---------------------------------------------------------------
    // registerAsAdmin – duplicate username
    // ---------------------------------------------------------------

    @Test
    void registerAsAdmin_duplicateUsername_throwsIllegalArgumentException() {
        when(userProfileRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.registerAsAdmin("admin", "Admin User", "admin@mail.com", "pass"));
    }

    // ---------------------------------------------------------------
    // findAllUsers
    // ---------------------------------------------------------------

    @Test
    void findAllUsers_returnsPaginatedResults() {
        UserProfile user = new UserProfile();
        user.setId("user-1");
        Page<UserProfile> page = new PageImpl<>(List.of(user));

        when(userProfileRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UserProfile> result = userProfileService.findAllUsers(0, 10);

        assertEquals(1, result.getTotalElements());
        verify(userProfileRepository).findAll(any(Pageable.class));
    }

    // ---------------------------------------------------------------
    // getByUsername
    // ---------------------------------------------------------------

    @Test
    void getByUsername_found() {
        UserProfile user = new UserProfile();
        user.setUsername("john");
        when(userProfileRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Optional<UserProfile> result = userProfileService.getByUsername("john");

        assertTrue(result.isPresent());
        assertEquals("john", result.get().getUsername());
    }

    @Test
    void getByUsername_notFound_returnsEmpty() {
        when(userProfileRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        Optional<UserProfile> result = userProfileService.getByUsername("nobody");

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // updateRole
    // ---------------------------------------------------------------

    @Test
    void updateRole_success() {
        UserProfile user = new UserProfile();
        user.setId("user-1");
        user.setRole(UserRole.CUSTOMER);

        when(userProfileRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = userProfileService.updateRole("user-1", UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, result.getRole());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateRole_userNotFound_throwsResourceNotFoundException() {
        when(userProfileRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updateRole("missing", UserRole.ADMIN));
    }

    // ---------------------------------------------------------------
    // deleteById – success
    // ---------------------------------------------------------------

    @Test
    void deleteById_success() {
        when(userProfileRepository.existsById("user-1")).thenReturn(true);

        userProfileService.deleteById("user-1");

        verify(userProfileRepository).deleteById("user-1");
    }

    // ---------------------------------------------------------------
    // updateProfile – user not found
    // ---------------------------------------------------------------

    @Test
    void updateProfile_userNotFound_throwsResourceNotFoundException() {
        when(userProfileRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updateProfile("missing", "Name", "email@mail.com"));
    }
}
