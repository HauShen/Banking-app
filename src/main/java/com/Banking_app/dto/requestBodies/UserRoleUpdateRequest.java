package com.Banking_app.dto.requestBodies;
import com.Banking_app.models.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
public class UserRoleUpdateRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}
