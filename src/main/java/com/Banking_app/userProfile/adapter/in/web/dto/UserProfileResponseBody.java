package com.Banking_app.userProfile.adapter.in.web.dto;
import com.Banking_app.userProfile.domain.enums.UserRole;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

public record UserProfileResponseBody(String id,
                                      String username,
                                      String fullName,
                                      String email,
                                      String role,
                                      Instant createdAt,
                                      Instant updatedAt) {
}
