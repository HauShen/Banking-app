package com.Banking_app.alert.application.port.out;

import com.Banking_app.alert.domain.models.TransactionAlert;

public interface SaveAlertPort {

    /** Inserts a new alert or updates a stored one; returns it with its id. */
    TransactionAlert save(TransactionAlert alert);
}
