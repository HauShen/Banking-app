package com.Banking_app.controllers;

import com.Banking_app.dto.*;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.UserRole;
import com.Banking_app.service.MyUserDetailsService;
import com.Banking_app.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(controllers = UserProfileController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.Banking_app.security.JwtRequestFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserProfileService userProfileService;
    @MockitoBean UserProfileMapper userProfileMapper;
    @MockitoBean MyUserDetailsService myUserDetailsService;

    @Test
    void register_shouldReturn201() throws Exception {
        UserProfileRequestBody req = new UserProfileRequestBody();
        req.setUsername("john");
        req.setFullName("John Doe");
        req.setEmail("john@mail.com");
        req.setPassword("password123");
        req.setRole(UserRole.CUSTOMER);

        UserProfile created = new UserProfile();
        created.setId("id-1");
        created.setUsername("john");

        UserProfileResponseBody res = UserProfileResponseBody.builder()
                .id("id-1").username("john").fullName("John Doe").email("john@mail.com")
                .role(UserRole.CUSTOMER).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(userProfileService.register(any(), any(), any(), any(), any())).thenReturn(created);
        when(userProfileMapper.toResponse(created)).thenReturn(res);

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        UserProfile user = new UserProfile();
        user.setId("id-1");
        user.setUsername("john");

        UserProfileResponseBody res = UserProfileResponseBody.builder()
                .id("id-1").username("john").fullName("John Doe").email("john@mail.com")
                .role(UserRole.CUSTOMER).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(userProfileService.getById("id-1")).thenReturn(Optional.of(user));
        when(userProfileMapper.toResponse(user)).thenReturn(res);

        mockMvc.perform(get("/api/users/id-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-1"));
    }
    @Test
    void register_invalidPassword_shouldReturn400() throws Exception {
        UserProfileRequestBody req = new UserProfileRequestBody();
        req.setUsername("john");
        req.setFullName("John Doe");
        req.setEmail("john@mail.com");
        req.setPassword("1234"); // invalid
        req.setRole(UserRole.CUSTOMER);

        mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be 8-100 characters"));
    }
}
