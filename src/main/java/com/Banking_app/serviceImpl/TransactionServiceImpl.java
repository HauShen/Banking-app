package com.Banking_app.serviceImpl;
import com.Banking_app.dto.mappers.TransactionMapper;
import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.jpaentities.Account;
import com.Banking_app.jpaentities.LedgerEntry;
import com.Banking_app.jpaentities.Transaction;
import com.Banking_app.enums.AccountStatus;
import com.Banking_app.enums.LedgerType;
import com.Banking_app.enums.TransactionStatus;
import com.Banking_app.repositories.AccountRepository;
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
    public TransferResponseBody transfer(TransferRequestBody transferRequestBody) {
        if (transferRequestBody.getFromAccountNumber().equals(transferRequestBody.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Idempotency guard
        var existingOpt = transactionRepository.findByIdempotencyKey(transferRequestBody.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();
            TransferResponseBody duplicatedResponse = new TransferResponseBody();
            duplicatedResponse.setTransactionReference(existing.getReferenceNumber());
            duplicatedResponse.setStatus(existing.getStatus().name());
            duplicatedResponse.setCreatedAt(existing.getCreatedAt());
            duplicatedResponse.setMessage("Duplicate request detected. Returning existing transaction.");
            return duplicatedResponse;
        }

        Account from = accountRepository.findByAccountNumber(transferRequestBody.getFromAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("From account not found"));
        Account to = accountRepository.findByAccountNumber(transferRequestBody.getToAccountNumber())
                .orElseThrow(() -> new EntityNotFoundException("To account not found"));


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
    @Override
    @Transactional(readOnly = true)
    public TransactionResponseBody getTransactionByReferenceNumber(String reference){
        Transaction transaction = transactionRepository.findByReferenceNumber(reference).orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        return TransactionMapper.toResponse(transaction);
    }
   @Override
   @Transactional(readOnly = true)
   public Page<TransactionResponseBody> getTransactionsByAccountNumber(String accountNumber, int page, int size) {
       accountRepository.findByAccountNumber(accountNumber)
               .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountNumber));

       Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
       Page<Transaction> transactions = transactionRepository.findAllByAccountNumber(accountNumber, pageable);
       return transactions.map(TransactionMapper::toResponse);
   }





}
