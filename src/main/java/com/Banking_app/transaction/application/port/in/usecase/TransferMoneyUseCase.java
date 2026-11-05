package com.Banking_app.transaction.application.port.in.usecase;

import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.TransferResult;

public interface TransferMoneyUseCase {
    TransferResult transfer(TransferMoneyCommand command);
}
