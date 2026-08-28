package com.Banking_app.userProfile.application.service;

import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.common.PageResult;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.Banking_app.userProfile.application.port.in.usecase.ListUserProfilesUseCase;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;

@Service
public class ListUserProfilesService implements ListUserProfilesUseCase {
    private final LoadUserProfilePort loadUserProfilePort;

    public ListUserProfilesService(LoadUserProfilePort loadUserProfilePort) {
        this.loadUserProfilePort = loadUserProfilePort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserProfile> list(ListUserProfilesQuery query) {
        return loadUserProfilePort.findAll(query);
    }
}
