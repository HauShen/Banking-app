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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserProfileServiceImplTest {
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserProfileServiceImpl userProfileService;
    @Test
    void register_success() {
        when(userProfileRepository.existsByUsername("john")).thenReturn(false);
        when(userProfileRepository.existsByEmail("john@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encoded1234");
        UserProfile saved = new UserProfile();
        saved.setId("id-1");
        saved.setUsername("john");
        saved.setEmail("john@mail.com");
        saved.setPassword("encoded1234");
        saved.setRole(UserRole.CUSTOMER);

        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(saved);
        UserProfile result = userProfileService.register("john", "John Doe", "john@mail.com", "1234", null);
        assertEquals("john", result.getUsername());
        assertEquals(UserRole.CUSTOMER, result.getRole());
        verify(passwordEncoder).encode("1234");
        verify(userProfileRepository).save(any(UserProfile.class));

    }
    @Test
    void getById_notFound_returnsEmpty(){
        when(userProfileRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<UserProfile> result = userProfileService.getById("missing");

        assertTrue(result.isEmpty());
    }
    @Test
    void updateProfile_success() {
        UserProfile user = new UserProfile();
        user.setId("id-1");
        user.setFullName("Old Name");
        user.setEmail("old@mail.com");

        when(userProfileRepository.findById("id-1")).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = userProfileService.updateProfile("id-1", "New Name", "new@mail.com");

        assertEquals("New Name", result.getFullName());
        assertEquals("new@mail.com", result.getEmail());
        assertNotNull(result.getUpdatedAt());
    }
    @Test
    void deleteById_notFound_shouldThrow() {
        when(userProfileRepository.existsById("id-404")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.deleteById("id-404"));
        verify(userProfileRepository, never()).deleteById(anyString());
    }

}
