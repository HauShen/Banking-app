package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.ReceivingAccount;
import com.Banking_app.transaction.application.port.out.ReceivingAccountPort;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stands in for the account module's receiving side: a few accounts with a balance and a locked flag. */
class FakeReceivingAccounts implements ReceivingAccountPort {

    private record Acc(Long id, String number, boolean locked) {
    }

    private final Map<String, Acc> byNumber = new LinkedHashMap<>();
    private final Map<Long, BigDecimal> balances = new LinkedHashMap<>();
    int lockCalls;
    int receiveCalls;

    void add(Long id, String number, boolean locked, String balance) {
        byNumber.put(number, new Acc(id, number, locked));
        balances.put(id, new BigDecimal(balance));
    }

    BigDecimal balanceOf(Long id) {
        return balances.get(id);
    }

    @Override
    public ReceivingAccount lockForReceiving(String accountNumber) {
        lockCalls++;
        Acc acc = byNumber.get(accountNumber);
        if (acc == null) {
            // the account module's TransferAccountNotFoundException is a 404 with this message
            throw new IllegalStateException("To account not found");
        }
        return new ReceivingAccount(acc.id(), acc.number(), acc.locked());
    }

    @Override
    public BigDecimal receive(Long accountId, BigDecimal amount) {
        receiveCalls++;
        BigDecimal after = balances.get(accountId).add(amount);
        balances.put(accountId, after);
        return after;
    }
}
