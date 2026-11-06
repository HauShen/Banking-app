package com.Banking_app.transaction.adapter.out.reference;

import com.Banking_app.transaction.application.port.out.TransactionReferencePort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Prefix + 12 upper-case hex characters from a random UUID: "TXN-3F9A0C12B7D4" for transfers, "CDM-3F9A0C12B7D4"
 * for top-ups — the same formats as the old code. 48 random bits; the unique constraint on reference_number catches
 * the (very unlikely) clash.
 */
@Component
public class RandomTransactionReferenceAdapter implements TransactionReferencePort {

    @Override
    public String next() {
        return "TXN-" + randomHex12();
    }

    @Override
    public String nextCashDeposit() {
        return "CDM-" + randomHex12();
    }

    private static String randomHex12() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
