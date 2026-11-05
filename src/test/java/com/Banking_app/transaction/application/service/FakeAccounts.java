package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.port.out.AccountDirectoryPort;
import com.Banking_app.transaction.application.port.out.AccountRef;
import com.Banking_app.transaction.application.port.out.TransferAccountsPort;
import com.Banking_app.transaction.application.port.out.TransferBalances;
import com.Banking_app.transaction.application.port.out.TransferParties;
import com.Banking_app.transaction.application.port.out.TransferParty;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Stands in for the account module: a few accounts with owner, balance and locked flag.
 * move() applies the same 20.00 minimum-balance rule (and message) as Account.withdraw.
 */
class FakeAccounts implements TransferAccountsPort, AccountDirectoryPort {

    static final class Acc {
        final Long id; final String number; final String owner; final boolean locked; BigDecimal balance;
        Acc(Long id, String number, String owner, boolean locked, String balance) {
            this.id = id; this.number = number; this.owner = owner; this.locked = locked;
            this.balance = new BigDecimal(balance);
        }
    }

    private final Map<Long, Acc> byId = new LinkedHashMap<>();
    int lockCalls;
    int moveCalls;
    int findByIdsCalls;

    void add(Long id, String number, String owner, boolean locked, String balance) {
        byId.put(id, new Acc(id, number, owner, locked, balance));
    }

    BigDecimal balanceOf(Long id) {
        return byId.get(id).balance;
    }

    private Acc byNumber(String number) {
        return byId.values().stream().filter(a -> a.number.equals(number)).findFirst()
                .orElseThrow(() -> new IllegalStateException("test setup: no account " + number));
    }

    private static TransferParty party(Acc a) {
        return new TransferParty(a.id, a.number, a.owner, a.locked);
    }

    private static AccountRef ref(Acc a) {
        return new AccountRef(a.id, a.number, a.owner, "MYR");
    }

    @Override
    public TransferParties lockPair(String fromAccountNumber, String toAccountNumber) {
        lockCalls++;
        return new TransferParties(party(byNumber(fromAccountNumber)), party(byNumber(toAccountNumber)));
    }

    @Override
    public TransferBalances move(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        moveCalls++;
        Acc from = byId.get(fromAccountId);
        Acc to = byId.get(toAccountId);
        if (from.balance.subtract(amount).compareTo(new BigDecimal("20.00")) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Minimum balance of $20.00 must be maintained.");
        }
        from.balance = from.balance.subtract(amount);
        to.balance = to.balance.add(amount);
        return new TransferBalances(from.balance, to.balance);
    }

    @Override
    public Optional<AccountRef> findByAccountNumber(String accountNumber) {
        return byId.values().stream().filter(a -> a.number.equals(accountNumber)).findFirst().map(FakeAccounts::ref);
    }

    @Override
    public Map<Long, AccountRef> findByIds(Collection<Long> accountIds) {
        findByIdsCalls++;
        return accountIds.stream().filter(byId::containsKey)
                .collect(Collectors.toMap(id -> id, id -> ref(byId.get(id))));
    }
}
