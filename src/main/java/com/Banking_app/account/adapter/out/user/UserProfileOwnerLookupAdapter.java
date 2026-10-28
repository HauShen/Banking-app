package com.Banking_app.account.adapter.out.user;

import com.Banking_app.account.application.port.out.AccountOwnerLookupPort;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.application.port.in.usecase.GetUserProfileUseCase;
import org.springframework.stereotype.Component;

/**
 * Answers "does this user exist?" by calling the userProfile module through its inbound
 * use case — never through its JPA repository or entities.
 */
@Component
public class UserProfileOwnerLookupAdapter implements AccountOwnerLookupPort {

    private final GetUserProfileUseCase getUserProfileUseCase;

    public UserProfileOwnerLookupAdapter(GetUserProfileUseCase getUserProfileUseCase) {
        this.getUserProfileUseCase = getUserProfileUseCase;
    }

    @Override
    public boolean ownerExists(String userId) {
        try {
            getUserProfileUseCase.byId(userId);
            return true;
        } catch (ResourceNotFoundException e) {
            // Note: byId is @Transactional, so this exception also marks the caller's transaction
            // rollback-only. That is fine today: every caller throws AccountOwnerNotFoundException
            // right after a false answer. Don't call this and then keep writing in the same transaction.
            return false;
        }
    }
}
