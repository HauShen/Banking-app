package com.Banking_app.fx.adapter.out.persistence;

import com.Banking_app.fx.adapter.out.persistence.jparepositories.LockedQuoteJpaRepository;
import com.Banking_app.fx.adapter.out.persistence.mappers.LockedQuotePersistenceMapper;
import com.Banking_app.fx.application.port.out.SaveLockedQuotePort;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.springframework.stereotype.Component;

/** Outbound adapter: locked quotes in the fx_quotes table. */
@Component
public class LockedQuotePersistenceAdapter implements SaveLockedQuotePort {

    private final LockedQuoteJpaRepository repository;
    private final LockedQuotePersistenceMapper mapper;

    public LockedQuotePersistenceAdapter(LockedQuoteJpaRepository repository, LockedQuotePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(LockedQuote quote) {
        // The id is set by the application, so Spring Data merges (one SELECT finds nothing, then the INSERT).
        // Flushed at once, so a constraint violation surfaces here and not at commit.
        repository.saveAndFlush(mapper.toJpaEntity(quote));
    }
}
