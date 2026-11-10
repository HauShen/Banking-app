package com.Banking_app.fx.adapter.out.persistence;

import com.Banking_app.fx.adapter.out.persistence.jparepositories.LockedQuoteJpaRepository;
import com.Banking_app.fx.adapter.out.persistence.mappers.LockedQuotePersistenceMapper;
import com.Banking_app.fx.application.port.out.LoadLockedQuotePort;
import com.Banking_app.fx.application.port.out.SaveLockedQuotePort;
import com.Banking_app.fx.domain.models.LockedQuote;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Outbound adapter: locked quotes in the fx_quotes table. */
@Component
public class LockedQuotePersistenceAdapter implements SaveLockedQuotePort, LoadLockedQuotePort {

    private final LockedQuoteJpaRepository repository;
    private final LockedQuotePersistenceMapper mapper;

    public LockedQuotePersistenceAdapter(LockedQuoteJpaRepository repository, LockedQuotePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(LockedQuote quote) {
        // The id is set by the application, so Spring Data merges: a new quote → INSERT, a used quote → UPDATE of the
        // row loaded (and locked) by loadForUpdate. Flushed at once, so a constraint violation surfaces here.
        repository.saveAndFlush(mapper.toJpaEntity(quote));
    }

    /** Must run inside a transaction (the caller's): the lock lasts until it ends. */
    @Override
    public Optional<LockedQuote> loadForUpdate(UUID quoteId) {
        return repository.findByIdForUpdate(quoteId.toString()).map(mapper::toDomain);
    }
}
