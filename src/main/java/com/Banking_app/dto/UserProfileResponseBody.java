package com.Banking_app.dto;
import com.Banking_app.models.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
