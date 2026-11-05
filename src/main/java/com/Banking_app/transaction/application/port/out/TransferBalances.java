package com.Banking_app.transaction.application.port.out;

import java.math.BigDecimal;

/** Both balances right after the money moved; they go into the ledger entries. */
public record TransferBalances(BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter) {
}
