package com.Banking_app.userProfile.application.port.out;

import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import com.Banking_app.userProfile.common.PageResult;
import java.util.Optional;

public interface LoadUserProfilePort {
    Optional<UserProfile> findById(String id);

    Optional<UserProfile> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<UserProfile> findByEmail(String email);

    /**Admin only

    /** True if at least one ADMIN exists. Used to make bootstrap a one-time operation. */
    boolean existsAnyAdmin();

    /** How many ADMIN accounts exist. Used to refuse the removal of the last one. */
    long countAdmins();

    PageResult<UserProfile> findAll(ListUserProfilesQuery query);


}
