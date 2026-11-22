package com.Banking_app.alert.application.port.out;

import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.alert.domain.models.TransactionAlert;
import com.Banking_app.shared.PageResult;

import java.time.Instant;
import java.util.Optional;

public interface LoadAlertPort {

    /** Newest first; {@code status} null = all. */
    PageResult<TransactionAlert> findPage(AlertStatus status, int page, int size);

    long countByStatus(AlertStatus status);

    Optional<TransactionAlert> findById(long id);

    /** Does the account already have an alert of this rule for a transfer after {@code after}? */
    boolean existsAfter(AlertRule rule, String accountNumber, Instant after);
}
