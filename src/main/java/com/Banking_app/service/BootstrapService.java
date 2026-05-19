package com.Banking_app.service;

import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.models.UserProfile;

public interface BootstrapService {
    BootstrapStatusResponse getStatus();
    UserProfile registerFirstAdmin(String username, String fullName, String email, String rawPassword);

}
