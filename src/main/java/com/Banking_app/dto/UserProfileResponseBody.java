package com.Banking_app.dto;
import com.Banking_app.models.enums.UserRole;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
@Value
@Builder
public class UserProfileResponseBody {
    String id;
    String username;
    String fullName;
    String email;
    UserRole role;
    Instant createdAt;
    Instant updatedAt;
}
