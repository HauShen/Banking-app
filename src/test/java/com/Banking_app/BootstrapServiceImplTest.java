package com.Banking_app;
import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import com.Banking_app.serviceImpl.BootstrapServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BootstrapServiceImplTest {
    @Mock
    private UserProfileJpaRepository userProfileJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BootstrapServiceImpl bootstrapService;

    // Helper: set the @Value field since @ExtendWith(MockitoExtension) doesn't inject @Value
    private void setBootstrapEnabled(boolean enabled) {
        ReflectionTestUtils.setField(bootstrapService, "bootstrapEnabled", enabled);
    }

    // ---------------------------------------------------------------
    // getStatus – bootstrap enabled, no admin yet → allowed = true
    // ---------------------------------------------------------------

    @Test
    void getStatus_bootstrapEnabled_noAdmin_allowedIsTrue() {
        setBootstrapEnabled(true);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userProfileJpaRepository.count()).thenReturn(0L);

        BootstrapStatusResponse status = bootstrapService.getStatus();

        assertTrue(status.isAllowed());
        assertFalse(status.isAdminExists());
        assertTrue(status.isBootstrapEnabled());
    }

    // ---------------------------------------------------------------
    // getStatus – bootstrap enabled, admin exists → allowed = false
    // ---------------------------------------------------------------

    @Test
    void getStatus_bootstrapEnabled_adminExists_allowedIsFalse() {
        setBootstrapEnabled(true);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);
        when(userProfileJpaRepository.count()).thenReturn(1L);

        BootstrapStatusResponse status = bootstrapService.getStatus();

        assertFalse(status.isAllowed());
        assertTrue(status.isAdminExists());
    }

    // ---------------------------------------------------------------
    // getStatus – bootstrap disabled → allowed = false regardless
    // ---------------------------------------------------------------

    @Test
    void getStatus_bootstrapDisabled_allowedIsFalse() {
        setBootstrapEnabled(false);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userProfileJpaRepository.count()).thenReturn(0L);

        BootstrapStatusResponse status = bootstrapService.getStatus();

        assertFalse(status.isAllowed());
        assertFalse(status.isBootstrapEnabled());
    }

    // ---------------------------------------------------------------
    // registerFirstAdmin – success
    // ---------------------------------------------------------------

    @Test
    void registerFirstAdmin_success() {
        setBootstrapEnabled(true);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userProfileJpaRepository.existsByUsername("admin")).thenReturn(false);
        when(userProfileJpaRepository.existsByEmail("admin@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("adminPass")).thenReturn("encodedAdminPass");

        UserProfileJpaEntity saved = new UserProfileJpaEntity();
        saved.setUsername("admin");
        saved.setRole(UserRole.ADMIN);
        when(userProfileJpaRepository.save(any(UserProfileJpaEntity.class))).thenReturn(saved);

        UserProfileJpaEntity result = bootstrapService.registerFirstAdmin(
                "admin", "Admin User", "admin@mail.com", "adminPass");

        assertNotNull(result);
        assertEquals(UserRole.ADMIN, result.getRole());
        verify(passwordEncoder).encode("adminPass");
        verify(userProfileJpaRepository).save(any(UserProfileJpaEntity.class));
    }

    // ---------------------------------------------------------------
    // registerFirstAdmin – bootstrap disabled
    // ---------------------------------------------------------------

    @Test
    void registerFirstAdmin_bootstrapDisabled_throwsIllegalStateException() {
        setBootstrapEnabled(false);

        assertThrows(IllegalStateException.class,
                () -> bootstrapService.registerFirstAdmin(
                        "admin", "Admin User", "admin@mail.com", "pass"));
        verify(userProfileJpaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // registerFirstAdmin – admin already exists
    // ---------------------------------------------------------------

    @Test
    void registerFirstAdmin_adminAlreadyExists_throwsIllegalStateException() {
        setBootstrapEnabled(true);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> bootstrapService.registerFirstAdmin(
                        "admin", "Admin User", "admin@mail.com", "pass"));
        verify(userProfileJpaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // registerFirstAdmin – duplicate username
    // ---------------------------------------------------------------

    @Test
    void registerFirstAdmin_duplicateUsername_throwsIllegalArgumentException() {
        setBootstrapEnabled(true);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userProfileJpaRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> bootstrapService.registerFirstAdmin(
                        "admin", "Admin User", "admin@mail.com", "pass"));
        verify(userProfileJpaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // registerFirstAdmin – duplicate email
    // ---------------------------------------------------------------

    @Test
    void registerFirstAdmin_duplicateEmail_throwsIllegalArgumentException() {
        setBootstrapEnabled(true);
        when(userProfileJpaRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userProfileJpaRepository.existsByUsername("admin")).thenReturn(false);
        when(userProfileJpaRepository.existsByEmail("admin@mail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> bootstrapService.registerFirstAdmin(
                        "admin", "Admin User", "admin@mail.com", "pass"));
        verify(userProfileJpaRepository, never()).save(any());
    }
}
