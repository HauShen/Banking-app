package com.Banking_app.userProfile.adapter.in.web.dto;
import com.Banking_app.userProfile.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class UserRoleUpdateRequestBody {
    @NotNull(message = "Role is required")
    private UserRole role;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
