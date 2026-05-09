package com.Banking_app.dto.mappers;

import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.models.Transaction;

public class TransactionMapper {
    public static TransactionResponseBody toResponse(Transaction transaction){
        return new TransactionResponseBody(
                transaction.getReferenceNumber(),
                transaction.getFromAccount().getAccountNumber(),
                transaction.getToAccount().getAccountNumber(),
                transaction.getToAccount().getUser().getId(),
                transaction.getAmount(),
                transaction.getFromAccount().getAccountCurrency().name(),
                transaction.getStatus().name(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getSuccessAt()
        );

    }
}
