package com.Banking_app.serviceImpl;

import com.Banking_app.dto.requestBodies.AccountRequestBody;
import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.models.Account;
import com.Banking_app.models.UserProfile;
import com.Banking_app.models.enums.AccountStatus;
import com.Banking_app.repositories.AccountRepository;
import com.Banking_app.repositories.UserProfileRepository;
import com.Banking_app.service.AccountService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.Banking_app.dto.mappers.AccountMapper;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,UserProfileRepository userProfileRepository){
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
    }
    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            // Example: 10-digit random number
            accountNumber = String.valueOf((long)(1000000000L + Math.random() * 9000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
    @Override
    public AccountResponseBody createAccountWithUserId(AccountRequestBody accountRequestBody){
        UserProfile user = userProfileRepository.findById(accountRequestBody.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + accountRequestBody.getUserId()));
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(accountRequestBody.getAccountType());
        account.setAccountStatus(accountRequestBody.getAccountStatus() != null ? accountRequestBody.getAccountStatus() : AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        account.setAccountNumber(generateUniqueAccountNumber()); // Generate unique account number in service

        Account savedAccount = accountRepository.save(account);

        // Map entity -> response DTO
        AccountResponseBody response = new AccountResponseBody();
        response.setAccountId(savedAccount.getAccountId());
        response.setUserId(savedAccount.getUser().getId());
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setAccountType(savedAccount.getAccountType());
        response.setAccountStatus(savedAccount.getAccountStatus());
        response.setCreatedAt(savedAccount.getCreatedAt());

    return response;
    }
    @Override
    @Transactional
    public AccountResponseBody getAccountByAccountId(Long accountId){
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));
        return AccountMapper.toResponse(account);
    }
    @Override
    @Transactional(readOnly = true)
    public AccountResponseBody getByAccountNumber(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with account number: " + accountNumber));
        return AccountMapper.toResponse(account);

    }
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseBody>getAllAccounts(){
        return accountRepository.findAll().stream().map(AccountMapper :: toResponse).toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseBody> getAccountsByStatus(AccountStatus status){
        return accountRepository.findByAccountStatus(status).stream().map(AccountMapper :: toResponse).toList();
    }
    @Override
    public AccountResponseBody updateAccountStatus(Long accountId,AccountStatus status){
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with account id: " + accountId));
        account.setAccountStatus(status);
        Account updated = accountRepository.save(account);
        return AccountMapper.toResponse(updated);
    }
    @Override
    public void deleteAccount(Long AccountId) {
        Account account = accountRepository.findById(AccountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + AccountId));
        accountRepository.delete(account);
    }



}
