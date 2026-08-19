package com.Banking_app.dto.responseBodies;
import com.Banking_app.userProfile.domain.enums.UserRole;
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
