package com.Banking_app.transaction.application.service;

import com.Banking_app.transaction.application.exception.IdempotencyKeyConflictException;
import com.Banking_app.transaction.application.exception.IdempotencyKeyMismatchException;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransferAccountLockedException;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;
import com.Banking_app.transaction.application.port.out.DuplicateIdempotencyKeyException;
import com.Banking_app.transaction.application.port.out.LoadTransactionPort;
import com.Banking_app.transaction.application.port.out.SaveTransactionPort;
import com.Banking_app.transaction.application.port.out.TransactionReferencePort;
import com.Banking_app.transaction.application.port.out.TransactionSummary;
import com.Banking_app.transaction.application.port.out.TransferAccountsPort;
import com.Banking_app.transaction.application.port.out.TransferBalances;
import com.Banking_app.transaction.application.port.out.TransferParties;
import com.Banking_app.transaction.application.port.out.TransferParty;
import com.Banking_app.transaction.domain.exception.SameAccountTransferException;
import com.Banking_app.transaction.domain.models.transaction.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Sends money between two accounts. Same checks, same order and same messages as the old
 * TransactionServiceImpl.transfer:
 * <ol>
 *   <li>same account → 400</li>
 *   <li>lock both accounts, lower number first (unknown → 404)</li>
 *   <li>only the from-account's owner, admins included → 403</li>
 *   <li>idempotency: same key from this account → the original transfer (409 if the amount or recipient differ);
 *       from another account → 400</li>
 *   <li>locked from / to → 400</li>
 *   <li>move the money (minimum balance → 400), record the transaction with its ledger entries</li>
 * </ol>
 * One database transaction for everything: if anything fails, nothing is stored and the balances stay as they were.
 * Two identical requests from one account run one after the other (both lock the from-account), so the second one
 * finds the first. Two accounts using the same key don't share a lock; if both get past the lookup, the unique
 * constraint stops the second insert and it gets the same 400 as if it had arrived a moment later.
 * (The old code's FAILED update was rolled back too, so dropping it changes nothing — plan decision 7.)
 */
@Service
public class TransferMoneyService implements TransferMoneyUseCase {

    private final TransferAccountsPort transferAccountsPort;
    private final LoadTransactionPort loadTransactionPort;
    private final SaveTransactionPort saveTransactionPort;
    private final TransactionReferencePort transactionReferencePort;
    private final Clock clock;

    public TransferMoneyService(TransferAccountsPort transferAccountsPort,
                                LoadTransactionPort loadTransactionPort,
                                SaveTransactionPort saveTransactionPort,
                                TransactionReferencePort transactionReferencePort,
                                Clock clock) {
        this.transferAccountsPort = transferAccountsPort;
        this.loadTransactionPort = loadTransactionPort;
        this.saveTransactionPort = saveTransactionPort;
        this.transactionReferencePort = transactionReferencePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TransferResult transfer(TransferMoneyCommand command) {
        if (command.fromAccountNumber().equals(command.toAccountNumber())) {
            throw new SameAccountTransferException();
        }

        TransferParties parties = transferAccountsPort.lockPair(command.fromAccountNumber(), command.toAccountNumber());
        TransferParty from = parties.from();
        TransferParty to = parties.to();

        // Only the owner can send money from an account. No admin exception: staff must not move a
        // customer's money through the customer API (same rule as top-up).
        if (!from.ownerId().equals(command.requesterUserId())) {
            throw new TransactionAccessDeniedException("You can only transfer from your own accounts");
        }

        // After the ownership check, so a known key can never reveal someone else's transaction.
        // After the lock, so two identical requests from the same account run one after the other.
        Optional<TransactionSummary> existing = loadTransactionPort.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            TransactionSummary original = existing.get();
            if (!original.isSentFrom(from.accountId())) {
                throw new IdempotencyKeyConflictException();
            }
            if (!original.toAccountId().equals(to.accountId()) || original.amount().compareTo(command.amount()) != 0) {
                throw new IdempotencyKeyMismatchException();   // same key, different transfer: a client mistake
            }
            return new TransferResult(original.referenceNumber(), original.status(), original.createdAt(), true);
        }

        if (from.locked()) {
            throw TransferAccountLockedException.from();
        }
        if (to.locked()) {
            throw TransferAccountLockedException.to();
        }

        Transaction pending = Transaction.startTransfer(transactionReferencePort.next(), from.accountId(),
                to.accountId(), command.amount(), command.description(), command.idempotencyKey(), clock.instant());

        TransferBalances balances = transferAccountsPort.move(from.accountId(), to.accountId(), command.amount());

        Transaction saved;
        try {
            saved = saveTransactionPort.save(
                    pending.completeTransfer(balances.fromBalanceAfter(), balances.toBalanceAfter(), clock.instant()));
        } catch (DuplicateIdempotencyKeyException otherAccountWasFirst) {
            // Another account stored the same key between our lookup and our insert. Throwing rolls back the money
            // we just moved; the answer is the same 400 as when the other transfer is found by the lookup.
            throw new IdempotencyKeyConflictException();
        }

        return new TransferResult(saved.getReferenceNumber(), saved.getStatus(), saved.getCreatedAt(), false);
    }
}
