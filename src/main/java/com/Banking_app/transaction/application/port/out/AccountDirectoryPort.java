package com.Banking_app.transaction.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Account lookups for reads. Implemented in step 8 by an adapter that calls LookupAccountsUseCase. */
public interface AccountDirectoryPort {

    Optional<AccountRef> findByAccountNumber(String accountNumber);

    /** One call for a whole page: account id → account. Unknown ids are left out. */
    Map<Long, AccountRef> findByIds(Collection<Long> accountIds);
}
