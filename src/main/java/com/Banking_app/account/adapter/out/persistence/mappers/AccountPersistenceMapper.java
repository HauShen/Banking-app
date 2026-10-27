package com.Banking_app.account.adapter.out.persistence.mappers;

import com.Banking_app.account.adapter.out.persistence.entities.AccountJpaEntity;
import com.Banking_app.account.domain.models.account.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {

    public Account toDomain(AccountJpaEntity entity) {
        return Account.rehydrate(
                entity.getAccountId(),
                entity.getUserId(),
                entity.getAccountNumber(),
                entity.getAccountType(),
                entity.getAccountStatus(),
                entity.getAccountCurrency(),
                entity.getCurrentBalance(),
                entity.getCreatedAt()
        );
    }

    /** id is null for a new account, so JPA inserts; otherwise it updates the existing row. */
    public AccountJpaEntity toJpaEntity(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setAccountId(account.getId());
        entity.setUserId(account.getOwnerId());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setAccountType(account.getType());
        entity.setAccountStatus(account.getStatus());
        entity.setAccountCurrency(account.getCurrency());
        entity.setCurrentBalance(account.getBalance());
        entity.setCreatedAt(account.getCreatedAt());
        return entity;
    }
}
