package com.Banking_app.dto.responseBodies;

import lombok.Data;

/**
 * Body of the public GET /api/auth/bootstrap-status. Public, so it says only what a first-run setup needs: whether
 * bootstrap is on, whether an admin exists, and so whether bootstrapping is still allowed. No user count: anyone
 * could read how many people use the bank.
 */
@Data
public class BootstrapStatusResponse {
    private boolean bootstrapEnabled;
    private boolean adminExists;
    private boolean allowed;

    public BootstrapStatusResponse(boolean bootstrapEnabled, boolean adminExists, boolean allowed) {
        this.bootstrapEnabled = bootstrapEnabled;
        this.adminExists = adminExists;
        this.allowed = allowed;
    }

    public BootstrapStatusResponse() {
    }
}
