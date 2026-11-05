package com.Banking_app.account.application.port.in.result;

import java.math.BigDecimal;

/** Balances right after MoveFundsUseCase.move; the transaction module writes them into the ledger entries. */
public record BalancesAfter(BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter) {
}
