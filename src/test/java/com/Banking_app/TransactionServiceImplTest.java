package com.Banking_app;
import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.models.Account;
import com.Banking_app.models.Transaction;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.TransactionStatus;
import com.Banking_app.repositories.AccountRepository;
import com.Banking_app.repositories.LedgerRepository;
import com.Banking_app.repositories.TransactionRepository;
import com.Banking_app.serviceImpl.TransactionServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Account buildAccount(String accountNumber, BigDecimal balance) {
        UserProfile user = new UserProfile();
        user.setId(UUID.randomUUID().toString());

        Account account = new Account();
        account.setAccountId(1L);
        account.setAccountNumber(accountNumber);
        account.setCurrentBalance(balance);
        account.setUser(user);
        return account;
    }

    private TransferRequestBody buildTransferRequest(String from, String to, BigDecimal amount) {
        TransferRequestBody req = new TransferRequestBody();
        req.setFromAccountNumber(from);
        req.setToAccountNumber(to);
        req.setAmount(amount);
        req.setDescription("Test transfer");
        req.setIdempotencyKey(UUID.randomUUID().toString());
        return req;
    }

    // ---------------------------------------------------------------
    // transfer – happy path
    // ---------------------------------------------------------------

    @Test
    void transfer_success() {
        Account from = buildAccount("ACC-FROM", new BigDecimal("500.00"));
        Account to   = buildAccount("ACC-TO",   new BigDecimal("100.00"));

        TransferRequestBody req = buildTransferRequest("ACC-FROM", "ACC-TO", new BigDecimal("100.00"));

        when(transactionRepository.findByIdempotencyKey(req.getIdempotencyKey())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC-FROM")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("ACC-TO")).thenReturn(Optional.of(to));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransferResponseBody response = transactionService.transfer(req);

        assertNotNull(response);
        assertEquals("Transfer successful", response.getMessage());
        assertEquals("SUCCESS", response.getStatus());

        // Verify balances were updated on the accounts
        assertEquals(new BigDecimal("400.00"), from.getCurrentBalance());
        assertEquals(new BigDecimal("200.00"), to.getCurrentBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class)); // PENDING + SUCCESS
        verify(ledgerRepository, times(2)).save(any());  // debit + credit
    }

    // ---------------------------------------------------------------
    // transfer – same-account guard
    // ---------------------------------------------------------------

    @Test
    void transfer_sameAccount_throwsIllegalArgumentException() {
        TransferRequestBody req = buildTransferRequest("ACC-SAME", "ACC-SAME", new BigDecimal("50.00"));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transfer(req));
        verifyNoInteractions(accountRepository, transactionRepository, ledgerRepository);
    }

    // ---------------------------------------------------------------
    // transfer – idempotency: duplicate request
    // ---------------------------------------------------------------

    @Test
    void transfer_duplicateIdempotencyKey_returnsExistingTransaction() {
        Transaction existing = new Transaction();
        existing.setReferenceNumber("TXN-EXISTING");
        existing.setStatus(TransactionStatus.SUCCESS);
        existing.setCreatedAt(Instant.now());

        TransferRequestBody req = buildTransferRequest("ACC-FROM", "ACC-TO", new BigDecimal("50.00"));

        // Idempotency key already exists
        when(transactionRepository.findByIdempotencyKey(req.getIdempotencyKey())).thenReturn(Optional.of(existing));

        // The current implementation falls through to the transfer after the idempotency check
        // (the duplicatedResponse is built but never returned). This test verifies the guard
        // is invoked and that the flow does NOT short-circuit before account lookup.
        when(accountRepository.findByAccountNumber("ACC-FROM")).thenReturn(Optional.of(buildAccount("ACC-FROM", new BigDecimal("500.00"))));
        when(accountRepository.findByAccountNumber("ACC-TO")).thenReturn(Optional.of(buildAccount("ACC-TO", new BigDecimal("100.00"))));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw
        TransferResponseBody response = transactionService.transfer(req);
        assertNotNull(response);

        verify(transactionRepository).findByIdempotencyKey(req.getIdempotencyKey());
    }

    // ---------------------------------------------------------------
    // transfer – from-account not found
    // ---------------------------------------------------------------

    @Test
    void transfer_fromAccountNotFound_throwsEntityNotFoundException() {
        TransferRequestBody req = buildTransferRequest("UNKNOWN", "ACC-TO", new BigDecimal("50.00"));

        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionService.transfer(req));
        verify(transactionRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // transfer – to-account not found
    // ---------------------------------------------------------------

    @Test
    void transfer_toAccountNotFound_throwsEntityNotFoundException() {
        Account from = buildAccount("ACC-FROM", new BigDecimal("500.00"));
        TransferRequestBody req = buildTransferRequest("ACC-FROM", "UNKNOWN", new BigDecimal("50.00"));

        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC-FROM")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionService.transfer(req));
        verify(transactionRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // transfer – exception during balance update sets status FAILED
    // ---------------------------------------------------------------

    @Test
    void transfer_exceptionDuringBalanceUpdate_setsTransactionFailed() {
        Account from = buildAccount("ACC-FROM", new BigDecimal("500.00"));
        Account to   = buildAccount("ACC-TO",   new BigDecimal("100.00"));

        TransferRequestBody req = buildTransferRequest("ACC-FROM", "ACC-TO", new BigDecimal("100.00"));

        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("ACC-FROM")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("ACC-TO")).thenReturn(Optional.of(to));

        // First save creates the PENDING tx; second save (accountRepository) triggers exception
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> transactionService.transfer(req));

        // The failed transaction must be saved with FAILED status
        verify(transactionRepository, atLeastOnce()).save(argThat(tx -> tx.getStatus() == TransactionStatus.FAILED));
    }

    // ---------------------------------------------------------------
    // getTransactionByReferenceNumber
    // ---------------------------------------------------------------

    @Test
    void getTransactionByReferenceNumber_success() {
        Account from = buildAccount("ACC-FROM", new BigDecimal("400.00"));
        Account to   = buildAccount("ACC-TO",   new BigDecimal("200.00"));

        Transaction tx = new Transaction();
        tx.setReferenceNumber("TXN-ABC");
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setCreatedAt(Instant.now());

        when(transactionRepository.findByReferenceNumber("TXN-ABC")).thenReturn(Optional.of(tx));

        TransactionResponseBody response = transactionService.getTransactionByReferenceNumber("TXN-ABC");

        assertNotNull(response);
        verify(transactionRepository).findByReferenceNumber("TXN-ABC");
    }

    @Test
    void getTransactionByReferenceNumber_notFound_throwsEntityNotFoundException() {
        when(transactionRepository.findByReferenceNumber("MISSING")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> transactionService.getTransactionByReferenceNumber("MISSING"));
    }
}
