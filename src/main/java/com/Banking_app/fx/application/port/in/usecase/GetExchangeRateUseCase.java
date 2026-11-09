package com.Banking_app.fx.application.port.in.usecase;

import com.Banking_app.fx.application.port.in.result.ExchangeRateView;

import java.util.List;

/**
 * The bank's exchange rates. Used by the web adapter (GET /api/fx/rates) and, from Phase 5B, by the transaction
 * module through its own out port (sell rate for transfers in a foreign currency, buy rate for remittances).
 */
public interface GetExchangeRateUseCase {

    /**
     * @throws com.Banking_app.fx.application.exception.UnsupportedCurrencyException (400) for MYR, an unknown code or
     *                                                                               a currency without a rate
     */
    ExchangeRateView getRate(String currencyCode);

    /** Every offered currency, sorted by code. */
    List<ExchangeRateView> listRates();
}
