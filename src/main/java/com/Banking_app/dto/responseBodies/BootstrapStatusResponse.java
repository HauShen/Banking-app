package com.Banking_app.dto.responseBodies;

import com.Banking_app.models.enums.UserRole;
import com.Banking_app.service.BootstrapService;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.time.Instant;

@Data
public class BootstrapStatusResponse {
    private boolean bootstrapEnabled;
    private boolean adminExists;
    private long userCount;
    private boolean allowed;
    public BootstrapStatusResponse(boolean bootstrapEnabled, boolean adminExists, long userCount, boolean allowed){
        this.bootstrapEnabled = bootstrapEnabled;
        this.adminExists = adminExists;
        this.userCount = userCount;
        this.allowed = allowed;

    }
    public BootstrapStatusResponse(){}

}
