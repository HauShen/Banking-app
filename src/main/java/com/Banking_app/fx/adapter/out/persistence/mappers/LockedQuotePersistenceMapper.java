package com.Banking_app.fx.adapter.out.persistence.mappers;

import com.Banking_app.fx.adapter.out.persistence.entities.LockedQuoteJpaEntity;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.UUID;

/** LockedQuote ↔ LockedQuoteJpaEntity. */
@Component
public class LockedQuotePersistenceMapper {

    public LockedQuoteJpaEntity toJpaEntity(LockedQuote quote) {
        LockedQuoteJpaEntity entity = new LockedQuoteJpaEntity();
        entity.setId(quote.getId().toString());
        entity.setUserId(quote.getOwnerId());
        entity.setCurrency(quote.getCurrency().getCurrencyCode());
        entity.setAmount(quote.getAmount());
        entity.setExchangeRate(quote.getExchangeRate());
        entity.setMyrAmount(quote.getMyrAmount());
        entity.setCreatedAt(quote.getCreatedAt());
        entity.setExpiresAt(quote.getExpiresAt());
        entity.setUsedAt(quote.getUsedAt());
        return entity;
    }

    public LockedQuote toDomain(LockedQuoteJpaEntity entity) {
        return LockedQuote.rehydrate(UUID.fromString(entity.getId()), entity.getUserId(),
                Currency.getInstance(entity.getCurrency()), entity.getAmount(), entity.getExchangeRate(),
                entity.getMyrAmount(), entity.getCreatedAt(), entity.getExpiresAt(), entity.getUsedAt());
    }
}
