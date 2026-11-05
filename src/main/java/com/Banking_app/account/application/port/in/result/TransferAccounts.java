package com.Banking_app.account.application.port.in.result;

/** Both accounts of a transfer, as locked by MoveFundsUseCase.lockPairForTransfer. */
public record TransferAccounts(AccountSnapshot from, AccountSnapshot to) {
}
