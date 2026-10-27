package com.Banking_app.account.adapter.out.persistence;

import com.Banking_app.account.adapter.out.persistence.jparepositories.AccountJpaRepository;
import com.Banking_app.account.adapter.out.persistence.mappers.AccountPersistenceMapper;
import com.Banking_app.account.application.port.out.DeleteAccountPort;
import com.Banking_app.account.application.port.out.LoadAccountPort;
import com.Banking_app.account.application.port.out.SaveAccountPort;
import com.Banking_app.account.domain.enums.AccountStatus;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountPersistenceAdapter implements LoadAccountPort, SaveAccountPort, DeleteAccountPort {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountPersistenceMapper mapper;

    public AccountPersistenceAdapter(AccountJpaRepository accountJpaRepository, AccountPersistenceMapper mapper) {
        this.accountJpaRepository = accountJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        return accountJpaRepository.findById(accountId).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountJpaRepository.findByAccountNumber(accountNumber).map(mapper::toDomain);
    }

    @Override
    public List<Account> findAll() {
        return accountJpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Account> findAllByStatus(AccountStatus status) {
        return accountJpaRepository.findAllByAccountStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Account> findAllByOwnerId(String ownerId) {
        return accountJpaRepository.findAllByUserId(ownerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountJpaRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    public Account save(Account account) {
        return mapper.toDomain(accountJpaRepository.save(mapper.toJpaEntity(account)));
    }

    @Override
    public void deleteById(Long accountId) {
        accountJpaRepository.deleteById(accountId);
    }
}
