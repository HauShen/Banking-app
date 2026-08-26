package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import com.Banking_app.userProfile.common.PageResult;


public interface ListUserProfilesUseCase {
    PageResult<UserProfile> list(ListUserProfilesQuery query);
}
