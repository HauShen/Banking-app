package com.Banking_app.service;

import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;

public interface BootstrapService {
    BootstrapStatusResponse getStatus();
    UserProfileJpaEntity registerFirstAdmin(String username, String fullName, String email, String rawPassword);

}
