package com.Banking_app.service;

import com.Banking_app.shared.Requester;
import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TransactionService {
    /**
     * @param currentUserId id of the logged-in user; must own the from-account.
     *                      Admins get no exception: staff cannot move a customer's money.
     */
    TransferResponseBody transfer(TransferRequestBody transferRequestBody, String currentUserId);
    /** Visible to the sender, the recipient, or an admin. */
    TransactionResponseBody getTransactionByReferenceNumber(String referenceNumber, Requester requester);

    /** Visible to the account owner or an admin. */
    Page<TransactionResponseBody> getTransactionsByAccountNumber(String accountNumber, int page, int size,
                                                                 Requester requester);
}
