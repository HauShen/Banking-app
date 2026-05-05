package com.Banking_app.service;

import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;

public interface TransactionService {
    TransferResponseBody transfer(TransferRequestBody transferRequestBody);
    TransactionResponseBody getTransactionByReferenceNumber(String referenceNumber);
}
