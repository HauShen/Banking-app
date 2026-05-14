package com.Banking_app.service;

import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.models.Transaction;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TransactionService {
    TransferResponseBody transfer(TransferRequestBody transferRequestBody);
    TransactionResponseBody getTransactionByReferenceNumber(String referenceNumber);

    //List<TransactionResponseBody> findAllTransactionsByAccountNumber(String accountNumber);
}
