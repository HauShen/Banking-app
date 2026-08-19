package com.Banking_app;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import com.Banking_app.userProfile.application.serviceImpl.UserProfileServiceImpl;
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
public class UserProfileJpaEntityServiceImplTest {
    @Mock
    private UserProfileJpaRepository userProfileJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    // ---------------------------------------------------------------
    // register – duplicate username
    // ---------------------------------------------------------------

    @Test
    void register_duplicateUsername_throwsIllegalArgumentException() {
        when(userProfileJpaRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.register("john", "John Doe", "john@mail.com", "pass"));
        verify(userProfileJpaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // register – duplicate email
    // ---------------------------------------------------------------

    @Test
    void register_duplicateEmail_throwsIllegalArgumentException() {
        when(userProfileJpaRepository.existsByUsername("john")).thenReturn(false);
        when(userProfileJpaRepository.existsByEmail("john@mail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.register("john", "John Doe", "john@mail.com", "pass"));
        verify(userProfileJpaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // registerAsAdmin – success
    // ---------------------------------------------------------------

    @Test
    void registerAsAdmin_success() {
        when(userProfileJpaRepository.existsByUsername("admin")).thenReturn(false);
        when(userProfileJpaRepository.existsByEmail("admin@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("adminPass")).thenReturn("encodedAdminPass");

        UserProfileJpaEntity saved = new UserProfileJpaEntity();
        saved.setId("admin-id");
        saved.setUsername("admin");
        saved.setRole(UserRole.ADMIN);
        when(userProfileJpaRepository.save(any(UserProfileJpaEntity.class))).thenReturn(saved);

        UserProfileJpaEntity result = userProfileService.registerAsAdmin("admin", "Admin User", "admin@mail.com", "adminPass");

        assertEquals(UserRole.ADMIN, result.getRole());
        verify(passwordEncoder).encode("adminPass");
    }

    // ---------------------------------------------------------------
    // registerAsAdmin – duplicate username
    // ---------------------------------------------------------------

    @Test
    void registerAsAdmin_duplicateUsername_throwsIllegalArgumentException() {
        when(userProfileJpaRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.registerAsAdmin("admin", "Admin User", "admin@mail.com", "pass"));
    }

    // ---------------------------------------------------------------
    // findAllUsers
    // ---------------------------------------------------------------

    @Test
    void findAllUsers_returnsPaginatedResults() {
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId("user-1");
        Page<UserProfileJpaEntity> page = new PageImpl<>(List.of(user));

        when(userProfileJpaRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<UserProfileJpaEntity> result = userProfileService.findAllUsers(0, 10);

        assertEquals(1, result.getTotalElements());
        verify(userProfileJpaRepository).findAll(any(Pageable.class));
    }

    // ---------------------------------------------------------------
    // getByUsername
    // ---------------------------------------------------------------

    @Test
    void getByUsername_found() {
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setUsername("john");
        when(userProfileJpaRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Optional<UserProfileJpaEntity> result = userProfileService.getByUsername("john");

        assertTrue(result.isPresent());
        assertEquals("john", result.get().getUsername());
    }

    @Test
    void getByUsername_notFound_returnsEmpty() {
        when(userProfileJpaRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        Optional<UserProfileJpaEntity> result = userProfileService.getByUsername("nobody");

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // updateRole
    // ---------------------------------------------------------------

    @Test
    void updateRole_success() {
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId("user-1");
        user.setRole(UserRole.CUSTOMER);

        when(userProfileJpaRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userProfileJpaRepository.save(any(UserProfileJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileJpaEntity result = userProfileService.updateRole("user-1", UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, result.getRole());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateRole_userNotFound_throwsResourceNotFoundException() {
        when(userProfileJpaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updateRole("missing", UserRole.ADMIN));
    }

    // ---------------------------------------------------------------
    // deleteById – success
    // ---------------------------------------------------------------

    @Test
    void deleteById_success() {
        when(userProfileJpaRepository.existsById("user-1")).thenReturn(true);

        userProfileService.deleteById("user-1");

        verify(userProfileJpaRepository).deleteById("user-1");
    }

    // ---------------------------------------------------------------
    // updateProfile – user not found
    // ---------------------------------------------------------------

    @Test
    void updateProfile_userNotFound_throwsResourceNotFoundException() {
        when(userProfileJpaRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updateProfile("missing", "Name", "email@mail.com"));
    }
}
