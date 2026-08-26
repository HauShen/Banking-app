package com.Banking_app.userProfile.application.service;

import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.application.port.in.usecase.GetUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserProfileService implements GetUserProfileUseCase {

    private final LoadUserProfilePort loadUserProfilePort;

    public GetUserProfileService(LoadUserProfilePort loadUserProfilePort) {
        this.loadUserProfilePort = loadUserProfilePort;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile byId(String id) {
        return loadUserProfilePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile byUsername(String username) {
        return loadUserProfilePort.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
