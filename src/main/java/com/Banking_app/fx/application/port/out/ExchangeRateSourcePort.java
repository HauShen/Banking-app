package com.Banking_app.fx.application.port.out;

import com.Banking_app.fx.domain.models.ExchangeRate;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Where the rates come from. Today: fixed values from configuration (ConfiguredExchangeRateSource). A live source
 * (e.g. a central-bank API with caching) can replace it later without touching the domain or the use cases.
 */
public interface ExchangeRateSourcePort {

    /** The rate for one currency, or empty if the bank doesn't offer it. */
    Optional<ExchangeRate> findRate(Currency currency);

    /** Every offered currency's rate, in no particular order. */
    List<ExchangeRate> allRates();
}
