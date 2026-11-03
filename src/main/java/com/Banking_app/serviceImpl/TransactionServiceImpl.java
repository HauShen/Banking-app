package com.Banking_app.serviceImpl;
import com.Banking_app.dto.mappers.TransactionMapper;
import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.account.adapter.out.persistence.entities.AccountJpaEntity;
import com.Banking_app.account.application.exception.AccountAccessDeniedException; // TODO Transaction module: own exception
import com.Banking_app.account.application.port.in.AccountRequester; // TODO Transaction module: shared requester type
import com.Banking_app.jpaentities.LedgerEntry;
import com.Banking_app.jpaentities.Transaction;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.enums.LedgerType;
import com.Banking_app.enums.TransactionStatus;
import com.Banking_app.account.adapter.out.persistence.jparepositories.AccountJpaRepository;
import com.Banking_app.repositories.LedgerRepository;
import com.Banking_app.repositories.TransactionRepository;
import com.Banking_app.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountJpaRepository accountRepository;
    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository, LedgerRepository ledgerRepository, AccountJpaRepository accountRepository){
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
    }
    public BigDecimal minimumBalance(){
        return new BigDecimal("20.00");
    }
    @Override
    @Transactional
    public TransferResponseBody transfer(TransferRequestBody transferRequestBody, String currentUserId) {
        if (transferRequestBody.getFromAccountNumber().equals(transferRequestBody.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Lock both account rows (SELECT ... FOR UPDATE) until this transaction ends, so a concurrent
        // top-up or transfer can't read the same old balance and overwrite ours.
        // Always lock in the same order (lower account number first): otherwise A->B and B->A at the
        // same moment could each hold one lock and wait for the other (deadlock), and PostgreSQL
        // would abort one of them with an error.
        String fromNumber = transferRequestBody.getFromAccountNumber();
        String toNumber = transferRequestBody.getToAccountNumber();
        AccountJpaEntity from;
        AccountJpaEntity to;
        if (fromNumber.compareTo(toNumber) < 0) {
            from = lockAccount(fromNumber, "From account not found");
            to = lockAccount(toNumber, "To account not found");
        } else {
            to = lockAccount(toNumber, "To account not found");
            from = lockAccount(fromNumber, "From account not found");
        }

        // Only the owner can send money from an account. No admin exception: staff must not be able
        // to move a customer's money through the customer API (same rule as top-up).
        if (!from.getUserId().equals(currentUserId)) {
            throw new AccountAccessDeniedException("You can only transfer from your own accounts");
        }

        // Idempotency guard. Runs AFTER the ownership check, so a duplicate key can never be used to
        // read someone else's transaction. The key must also belong to a transfer from this same
        // account: reusing another account's key is rejected instead of returning that transaction.
        var existingOpt = transactionRepository.findByIdempotencyKey(transferRequestBody.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();
            AccountJpaEntity existingFrom = existing.getFromAccount();
            if (existingFrom == null || !existingFrom.getAccountId().equals(from.getAccountId())) {
                throw new IllegalArgumentException("idempotencyKey has already been used for a different transfer");
            }
            TransferResponseBody duplicatedResponse = new TransferResponseBody();
            duplicatedResponse.setTransactionReference(existing.getReferenceNumber());
            duplicatedResponse.setStatus(existing.getStatus().name());
            duplicatedResponse.setCreatedAt(existing.getCreatedAt());
            duplicatedResponse.setMessage("Duplicate request detected. Returning existing transaction.");
            return duplicatedResponse;
        }

        if(from.getAccountStatus().name().equals(AccountStatus.LOCKED.name())){
            throw new IllegalArgumentException("Cannot process transfer with an locked account.");
        }
        if(to.getAccountStatus().name().equals(AccountStatus.LOCKED.name())){
            throw new IllegalArgumentException("Selected account has been locked");
        }


        BigDecimal remainingBalance = from.getCurrentBalance().subtract(transferRequestBody.getAmount());
        if (remainingBalance.compareTo(minimumBalance()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Minimum balance of $20.00 must be maintained.");
        }

        String reference = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Transaction tx = new Transaction();
        tx.setReferenceNumber(reference);
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(transferRequestBody.getAmount());
        tx.setDescription(transferRequestBody.getDescription());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setCreatedAt(Instant.now());
        tx.setIdempotencyKey(transferRequestBody.getIdempotencyKey());
        tx = transactionRepository.save(tx);

        try {
            //  Update balances FIRST so balanceAfter values are correct
            from.setCurrentBalance(from.getCurrentBalance().subtract(transferRequestBody.getAmount()));
            to.setCurrentBalance(to.getCurrentBalance().add(transferRequestBody.getAmount()));
            accountRepository.save(from);
            accountRepository.save(to);

            //  Now record ledger entries WITH balanceAfter set
            LedgerEntry debit = new LedgerEntry();
            debit.setTransaction(tx);
            debit.setAccount(from);
            debit.setLedgerType(LedgerType.DEBIT);
            debit.setAmount(transferRequestBody.getAmount());
            debit.setBalanceAfter(from.getCurrentBalance());
            debit.setPostedAt(Instant.now());

            LedgerEntry credit = new LedgerEntry();
            credit.setTransaction(tx);
            credit.setAccount(to);
            credit.setLedgerType(LedgerType.CREDIT);
            credit.setAmount(transferRequestBody.getAmount());
            credit.setBalanceAfter(to.getCurrentBalance());
            credit.setPostedAt(Instant.now());

            ledgerRepository.save(debit);
            ledgerRepository.save(credit);

            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setSuccessAt(Instant.now());
            transactionRepository.save(tx);

            TransferResponseBody response = new TransferResponseBody();
            response.setTransactionReference(reference);
            response.setStatus(tx.getStatus().name());
            response.setCreatedAt(Instant.now());
            response.setMessage("Transfer successful");
            return response;

        } catch (Exception ex) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw ex;
        }
    }
    private AccountJpaEntity lockAccount(String accountNumber, String notFoundMessage) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException(notFoundMessage));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseBody getTransactionByReferenceNumber(String reference, AccountRequester requester){
        Transaction transaction = transactionRepository.findByReferenceNumber(reference).orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        // The sender and the recipient may both see a transfer; admins see everything.
        // fromAccount is null for cash deposits (top-ups), so only the recipient side applies there.
        AccountJpaEntity fromAccount = transaction.getFromAccount();
        boolean isSender = fromAccount != null && fromAccount.getUserId().equals(requester.userId());
        boolean isRecipient = transaction.getToAccount().getUserId().equals(requester.userId());
        if (!requester.admin() && !isSender && !isRecipient) {
            throw new AccountAccessDeniedException("You can only view your own transactions");
        }

        return TransactionMapper.toResponse(transaction);
    }
   @Override
   @Transactional(readOnly = true)
   public Page<TransactionResponseBody> getTransactionsByAccountNumber(String accountNumber, int page, int size,
                                                                       AccountRequester requester) {
       AccountJpaEntity account = accountRepository.findByAccountNumber(accountNumber)
               .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountNumber));

       // Same rule as GetAccountService: the owner or an admin.
       if (!requester.canAccessAccountsOf(account.getUserId())) {
           throw new AccountAccessDeniedException("You can only view the history of your own accounts");
       }

       Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
       Page<Transaction> transactions = transactionRepository.findAllByAccountNumber(accountNumber, pageable);
       return transactions.map(TransactionMapper::toResponse);
   }





}
