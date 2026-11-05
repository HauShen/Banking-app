package com.Banking_app.transaction.application.port.in.usecase;

import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.port.in.result.TransactionView;

public interface GetTransactionUseCase {
    /** Allowed for the sender, the recipient and admins. */
    TransactionView getByReference(String referenceNumber, Requester requester);
}
