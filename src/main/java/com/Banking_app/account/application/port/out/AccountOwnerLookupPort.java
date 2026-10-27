package com.Banking_app.account.application.port.out;

/** Asks the userProfile module whether a user exists. The account module never touches user tables. */
public interface AccountOwnerLookupPort {
    boolean ownerExists(String userId);
}
