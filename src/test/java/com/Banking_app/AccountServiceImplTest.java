package com.Banking_app;

import com.Banking_app.dto.requestBodies.AccountRequestBody;
import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.models.Account;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.AccountStatus;
import com.Banking_app.models.enums.AccountType;
import com.Banking_app.repositories.AccountRepository;
import com.Banking_app.repositories.UserProfileRepository;
import com.Banking_app.serviceImpl.AccountServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private UserProfile buildUser(String id) {
        UserProfile user = new UserProfile();
        user.setId(id);
        user.setUsername("user_" + id);
        return user;
    }

    private Account buildAccount(Long id, String accountNumber, UserProfile user) {
        Account account = new Account();
        account.setAccountId(id);
        account.setAccountNumber(accountNumber);
        account.setUser(user);
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setCurrentBalance(new BigDecimal("20.00"));
        account.setCreatedAt(Instant.now());
        return account;
    }

    // ---------------------------------------------------------------
    // createAccountWithUserId
    // ---------------------------------------------------------------

    @Test
    void createAccount_success() {
        UserProfile user = buildUser("user-1");

        AccountRequestBody requestBody = new AccountRequestBody();
        requestBody.setUserId("user-1");
        requestBody.setAccountType(AccountType.SAVINGS);

        when(userProfileRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        Account savedAccount = buildAccount(1L, "1234567890", user);
        savedAccount.setAccountType(AccountType.SAVINGS);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AccountResponseBody response = accountService.createAccountWithUserId(requestBody);

        assertNotNull(response);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_userNotFound_throwsEntityNotFoundException() {
        AccountRequestBody requestBody = new AccountRequestBody();
        requestBody.setUserId("missing-user");

        when(userProfileRepository.findById("missing-user")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.createAccountWithUserId(requestBody));
        verify(accountRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // getAccountByAccountId
    // ---------------------------------------------------------------

    @Test
    void getAccountByAccountId_success() {
        UserProfile user = buildUser("user-1");
        Account account = buildAccount(1L, "1234567890", user);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountResponseBody response = accountService.getAccountByAccountId(1L);

        assertNotNull(response);
        verify(accountRepository).findById(1L);
    }

    @Test
    void getAccountByAccountId_notFound_throwsEntityNotFoundException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.getAccountByAccountId(99L));
    }

    // ---------------------------------------------------------------
    // getByAccountNumber
    // ---------------------------------------------------------------

    @Test
    void getByAccountNumber_success() {
        UserProfile user = buildUser("user-1");
        Account account = buildAccount(1L, "ACC-001", user);

        when(accountRepository.findByAccountNumber("ACC-001")).thenReturn(Optional.of(account));

        AccountResponseBody response = accountService.getByAccountNumber("ACC-001");

        assertNotNull(response);
    }

    @Test
    void getByAccountNumber_notFound_throwsEntityNotFoundException() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.getByAccountNumber("UNKNOWN"));
    }

    // ---------------------------------------------------------------
    // getAllAccounts
    // ---------------------------------------------------------------

    @Test
    void getAllAccounts_returnsAllAccounts() {
        UserProfile user = buildUser("user-1");
        List<Account> accounts = List.of(
                buildAccount(1L, "ACC-001", user),
                buildAccount(2L, "ACC-002", user)
        );

        when(accountRepository.findAll()).thenReturn(accounts);

        List<AccountResponseBody> result = accountService.getAllAccounts();

        assertEquals(2, result.size());
        verify(accountRepository).findAll();
    }

    @Test
    void getAllAccounts_emptyList_returnsEmpty() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<AccountResponseBody> result = accountService.getAllAccounts();

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // getAllAccountsByUserId
    // ---------------------------------------------------------------

    @Test
    void getAllAccountsByUserId_success() {
        UserProfile user = buildUser("user-1");
        List<Account> accounts = List.of(buildAccount(1L, "ACC-001", user));

        when(userProfileRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(accountRepository.findAllByUserId("user-1")).thenReturn(accounts);

        List<AccountResponseBody> result = accountService.getAllAccountsByUserId("user-1");

        assertEquals(1, result.size());
    }

    @Test
    void getAllAccountsByUserId_userNotFound_throwsEntityNotFoundException() {
        when(userProfileRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.getAllAccountsByUserId("missing"));
    }

    // ---------------------------------------------------------------
    // getAccountsByStatus
    // ---------------------------------------------------------------

    @Test
    void getAccountsByStatus_returnsFilteredAccounts() {
        UserProfile user = buildUser("user-1");
        Account account = buildAccount(1L, "ACC-001", user);

        when(accountRepository.findByAccountStatus(AccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));

        List<AccountResponseBody> result = accountService.getAccountsByStatus(AccountStatus.ACTIVE);

        assertEquals(1, result.size());
        verify(accountRepository).findByAccountStatus(AccountStatus.ACTIVE);
    }

    // ---------------------------------------------------------------
    // updateAccountStatus
    // ---------------------------------------------------------------

    @Test
    void updateAccountStatus_success() {
        UserProfile user = buildUser("user-1");
        Account account = buildAccount(1L, "ACC-001", user);
        account.setAccountStatus(AccountStatus.ACTIVE);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponseBody response = accountService.updateAccountStatus(1L, AccountStatus.LOCKED);

        assertNotNull(response);
        verify(accountRepository).save(argThat(a -> a.getAccountStatus() == AccountStatus.LOCKED));
    }

    @Test
    void updateAccountStatus_accountNotFound_throwsEntityNotFoundException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.updateAccountStatus(99L, AccountStatus.LOCKED));
    }

    // ---------------------------------------------------------------
    // deleteAccount
    // ---------------------------------------------------------------

    @Test
    void deleteAccount_success() {
        UserProfile user = buildUser("user-1");
        Account account = buildAccount(1L, "ACC-001", user);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        accountService.deleteAccount(1L);

        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_accountNotFound_throwsEntityNotFoundException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> accountService.deleteAccount(99L));
        verify(accountRepository, never()).delete(any());
    }
}
