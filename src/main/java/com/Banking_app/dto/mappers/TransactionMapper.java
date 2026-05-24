package com.Banking_app.dto.mappers;

import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.models.Transaction;

public class TransactionMapper {
    public static TransactionResponseBody toResponse(Transaction transaction){
        //  Safe null check — fromAccount is null for CDM top-ups
        String fromAccountNumber = transaction.getFromAccount() != null
                ? transaction.getFromAccount().getAccountNumber()
                : "Cash Deposit Machine";

        String currency = transaction.getFromAccount() != null
                ? transaction.getFromAccount().getAccountCurrency().name()
                : transaction.getToAccount().getAccountCurrency().name();

        return new TransactionResponseBody(
                transaction.getReferenceNumber(),
                fromAccountNumber,                              //  use safe variable
                transaction.getToAccount().getAccountNumber(),
                transaction.getToAccount().getUser().getId(),
                transaction.getAmount(),
                currency,                                       //  use safe variable
                transaction.getStatus().name(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getSuccessAt()
        );
    }
}
