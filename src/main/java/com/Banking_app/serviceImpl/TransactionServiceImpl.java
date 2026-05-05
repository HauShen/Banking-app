package com.Banking_app.serviceImpl;
import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.models.Account;
import com.Banking_app.models.LedgerEntry;
import com.Banking_app.models.Transaction;
import com.Banking_app.models.enums.LedgerType;
import com.Banking_app.models.enums.TransactionStatus;
import com.Banking_app.repositories.AccountRepository;
import com.Banking_app.repositories.LedgerRepository;
import com.Banking_app.repositories.TransactionRepository;
import com.Banking_app.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;
    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository, LedgerRepository ledgerRepository, AccountRepository accountRepository){
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.accountRepository = accountRepository;
    }
    public BigDecimal minimumBalance(){
        return new BigDecimal("20.00");
    }
    @Override
    @Transactional
    public TransferResponseBody transfer(TransferRequestBody transferRequestBody){
        if(transferRequestBody.getFromAccountId().equals(transferRequestBody.getToAccountId())){
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        // 1) Idempotency guard: if same key already processed, return old result
        //    Same idempotencyKey = same transaction result, no double debit.
        var existingOpt = transactionRepository.findByIdempotencyKey(transferRequestBody.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();
            TransferResponseBody duplicatedResponse = new TransferResponseBody();
            duplicatedResponse.setTransactionReference(existing.getReferenceNumber());
            duplicatedResponse.setStatus(existing.getStatus().name());
            duplicatedResponse.setCreatedAt(existing.getCreatedAt());
            duplicatedResponse.setMessage("Duplicate request detected. Returning existing transaction.");
        }


        // Starting transfer.
        Account from = accountRepository.findById(transferRequestBody.getFromAccountId()).orElseThrow(() -> new EntityNotFoundException("From account not found"));
        Account to = accountRepository.findById(transferRequestBody.getToAccountId()) .orElseThrow(() -> new EntityNotFoundException("To account not found"));

        BigDecimal minimumRemainingBalance = minimumBalance();
        BigDecimal remainingBalance = from.getCurrentBalance().subtract(transferRequestBody.getAmount());
        if(minimumRemainingBalance.compareTo(minimumRemainingBalance) < 0){  // If sender would end with 19.99 or less → reject.
            throw new IllegalArgumentException("Insufficient balance");     // If sender ends with 20.00 or more → allow.
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

        // Balance update
        try {
            from.setCurrentBalance(from.getCurrentBalance().subtract(transferRequestBody.getAmount()));
            to.setCurrentBalance(to.getCurrentBalance().add(transferRequestBody.getAmount()));
            accountRepository.save(from);
            accountRepository.save(to);

            // Double-entry ledger
            LedgerEntry debit = new LedgerEntry();
            debit.setTransaction(tx);
            debit.setAccount(from);
            debit.setLedgerType(LedgerType.DEBIT);
            debit.setAmount(transferRequestBody.getAmount());
            debit.setPostedAt(Instant.now());

            LedgerEntry credit = new LedgerEntry();
            credit.setTransaction(tx);
            credit.setAccount(to);
            credit.setLedgerType(LedgerType.CREDIT);
            credit.setAmount(transferRequestBody.getAmount());
            credit.setPostedAt(Instant.now());

            ledgerRepository.save(debit);
            ledgerRepository.save(credit);

            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setSuccessAt(Instant.now());
            transactionRepository.save(tx);

            TransferResponseBody currentTransferResponseBody = new TransferResponseBody();
            currentTransferResponseBody.setTransactionReference(reference);
            currentTransferResponseBody.setStatus(tx.getStatus().name());
            currentTransferResponseBody.setCreatedAt(Instant.now());
            currentTransferResponseBody.setMessage("Transfer successful");

            return currentTransferResponseBody;
        }
        catch (Exception ex){
        tx.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(tx);
        throw ex;
        }
    }
    @Override
    @Transactional(readOnly = true)
    public TransactionResponseBody getTransactionByReferenceNumber(String reference){
        Transaction transaction = transactionRepository.findByReferenceNumber(reference).orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        TransactionResponseBody currentTransactionResponseBody = new TransactionResponseBody();
        currentTransactionResponseBody.setReference(transaction.getReferenceNumber());
        currentTransactionResponseBody.setFromAccountId(transaction.getFromAccount().getAccountId());
        currentTransactionResponseBody.setToAccountId(transaction.getToAccount().getAccountId());
        currentTransactionResponseBody.setAmount(transaction.getAmount());
        currentTransactionResponseBody.setStatus(transaction.getStatus().name());
        currentTransactionResponseBody.setDescription(transaction.getDescription());
        currentTransactionResponseBody.setCreatedAt(transaction.getCreatedAt());
        currentTransactionResponseBody.setPostedAt(transaction.getSuccessAt());

        return currentTransactionResponseBody;
    }


}
