package com.Banking_app.account.adapter.out.ledger;

import com.Banking_app.account.adapter.out.persistence.entities.AccountJpaEntity;
import com.Banking_app.account.adapter.out.persistence.jparepositories.AccountJpaRepository;
import com.Banking_app.account.application.port.out.RecordCashDepositPort;
import com.Banking_app.enums.LedgerType;
import com.Banking_app.enums.TransactionStatus;
import com.Banking_app.jpaentities.LedgerEntry;
import com.Banking_app.jpaentities.Transaction;
import com.Banking_app.repositories.LedgerRepository;
import com.Banking_app.repositories.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Writes a Cash Deposit Machine top-up into the legacy transactions + ledgers tables,
 * exactly like the old AccountServiceImpl.topUp did.
 * TODO: when the Transaction module goes hexagonal, call its use case here instead of its repositories.
 */
@Component
public class LegacyCashDepositAdapter implements RecordCashDepositPort {

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountJpaRepository accountJpaRepository;

    public LegacyCashDepositAdapter(TransactionRepository transactionRepository,
                                    LedgerRepository ledgerRepository,
                                    AccountJpaRepository accountJpaRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public void recordCashDeposit(Long accountId, BigDecimal amount, BigDecimal balanceAfter, Instant depositedAt) {
        AccountJpaEntity account = accountJpaRepository.getReferenceById(accountId);

        // 1) Transaction record — fromAccount = null means Cash Deposit Machine
        String reference = "CDM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Transaction tx = new Transaction();
        tx.setReferenceNumber(reference);
        tx.setFromAccount(null);
        tx.setToAccount(account);
        tx.setAmount(amount);
        tx.setDescription("Cash Deposit Machine top-up");
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setCreatedAt(depositedAt);
        tx.setSuccessAt(depositedAt);
        tx.setIdempotencyKey("CDM-" + UUID.randomUUID());   // unique per top-up
        tx = transactionRepository.save(tx);

        // 2) CREDIT ledger entry for the account
        LedgerEntry credit = new LedgerEntry();
        credit.setTransaction(tx);
        credit.setAccount(account);
        credit.setLedgerType(LedgerType.CREDIT);
        credit.setAmount(amount);
        credit.setBalanceAfter(balanceAfter);
        credit.setPostedAt(depositedAt);
        ledgerRepository.save(credit);
    }
}
