package com.Banking_app.dto.requestBodies;
import com.Banking_app.userProfile.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}
