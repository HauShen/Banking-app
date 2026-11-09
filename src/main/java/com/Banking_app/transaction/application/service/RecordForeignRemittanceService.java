package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.exception.IdempotencyKeyMismatchException;
import com.Banking_app.transaction.application.exception.TransferAccountLockedException;
import com.Banking_app.transaction.application.port.in.command.RecordForeignRemittanceCommand;
import com.Banking_app.transaction.application.port.in.result.RemittanceResult;
import com.Banking_app.transaction.application.port.in.usecase.RecordForeignRemittanceUseCase;
import com.Banking_app.transaction.application.port.out.CurrencyConversionPort;
import com.Banking_app.transaction.application.port.out.DuplicateIdempotencyKeyException;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import com.Banking_app.transaction.application.port.out.ReceivingAccount;
import com.Banking_app.transaction.application.port.out.ReceivingAccountPort;
import com.Banking_app.transaction.application.port.out.SaveTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionReferencePort;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import com.Banking_app.transaction.domain.enums.TransactionType;
import com.Banking_app.transaction.domain.models.transaction.CurrencyConversion;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

/**
 * Credits money from abroad, in the same order as a transfer:
 * <ol>
 *   <li>lock the receiving account (unknown → 404)</li>
 *   <li>idempotency: the same remittance again (same account, currency and amount) → the original;
 *       the key used for anything else → 409</li>
 *   <li>locked account → 400 "Selected account has been locked" (as for a transfer's recipient)</li>
 *   <li>convert at the bank's BUY rate (fx module: MYR, currency not offered, too many decimals → 400)</li>
 *   <li>credit the MYR amount, record a FOREIGN_REMITTANCE with one CREDIT ledger entry</li>
 * </ol>
 * One database transaction: if anything fails, nothing is stored and the balance stays as it was.
 * Two identical requests run one after the other (both lock the account), so the second finds the first.
 */
@Service
public class RecordForeignRemittanceService implements RecordForeignRemittanceUseCase {

    static final String DESCRIPTION_PREFIX = "Foreign remittance from ";

    private final ReceivingAccountPort receivingAccountPort;
    private final LoadTransactionPort loadTransactionPort;
    private final SaveTransactionPort saveTransactionPort;
    private final TransactionReferencePort transactionReferencePort;
    private final CurrencyConversionPort currencyConversionPort;
    private final Clock clock;

    public RecordForeignRemittanceService(ReceivingAccountPort receivingAccountPort,
                                          LoadTransactionPort loadTransactionPort,
                                          SaveTransactionPort saveTransactionPort,
                                          TransactionReferencePort transactionReferencePort,
                                          CurrencyConversionPort currencyConversionPort,
                                          Clock clock) {
        this.receivingAccountPort = receivingAccountPort;
        this.loadTransactionPort = loadTransactionPort;
        this.saveTransactionPort = saveTransactionPort;
        this.transactionReferencePort = transactionReferencePort;
        this.currencyConversionPort = currencyConversionPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RemittanceResult receive(RecordForeignRemittanceCommand command) {
        ReceivingAccount account = receivingAccountPort.lockForReceiving(command.toAccountNumber());

        // After the lock, so two identical requests run one after the other; before the conversion, so a retry gets
        // the original even if the rate has changed since.
        Optional<TransactionSummary> existing = loadTransactionPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            TransactionSummary original = existing.get();
            if (!isSameRemittance(original, account.accountId(), command)) {
                throw IdempotencyKeyMismatchException.forRemittance();
            }
            return new RemittanceResult(original.referenceNumber(), original.status(), original.createdAt(),
                    account.accountNumber(), original.amount(),
                    TransactionViews.foreignAmountOf(original.conversion()), true);
        }

        if (account.locked()) {
            throw TransferAccountLockedException.to();
        }

        CurrencyConversion conversion = currencyConversionPort.convertIncoming(command.currency(), command.amount());
        Transaction pending = Transaction.startForeignRemittance(transactionReferencePort.nextForeignRemittance(),
                account.accountId(), conversion, DESCRIPTION_PREFIX + command.senderName(), command.idempotencyKey(),
                clock.instant());

        BigDecimal balanceAfter = receivingAccountPort.receive(account.accountId(), pending.getAmount());

        Transaction saved;
        try {
            saved = saveTransactionPort.save(pending.completeForeignRemittance(balanceAfter, clock.instant()));
        } catch (DuplicateIdempotencyKeyException otherRequestWasFirst) {
            // A request for another account stored the same key between our lookup and our insert (requests for the
            // same account wait for our lock). Throwing rolls back the credit; same answer as a later request.
            throw IdempotencyKeyMismatchException.forRemittance();
        }

        return new RemittanceResult(saved.getReferenceNumber(), saved.getStatus(), saved.getCreatedAt(),
                account.accountNumber(), saved.getAmount(),
                TransactionViews.foreignAmountOf(saved.getConversion().orElse(null)), false);
    }

    /** Same remittance = a remittance to the same account with the same original currency and amount. */
    private static boolean isSameRemittance(TransactionSummary original, Long accountId,
                                            RecordForeignRemittanceCommand command) {
        CurrencyConversion conversion = original.conversion();
        return original.type() == TransactionType.FOREIGN_REMITTANCE
                && original.toAccountId().equals(accountId)
                && conversion != null
                && conversion.originalCurrency().getCurrencyCode().equals(command.currency())
                && conversion.originalAmount().compareTo(command.amount()) == 0;
    }
}
