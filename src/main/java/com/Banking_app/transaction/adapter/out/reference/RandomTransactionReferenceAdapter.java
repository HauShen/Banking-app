package com.Banking_app.transaction.adapter.out.reference;

import com.Banking_app.transaction.application.port.out.TransactionReferencePort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * "TXN-" + 12 upper-case hex characters from a random UUID, e.g. "TXN-3F9A0C12B7D4" — the same format as the old
 * TransactionServiceImpl. 48 random bits; the unique constraint on reference_number catches the (very unlikely) clash.
 */
@Component
public class RandomTransactionReferenceAdapter implements TransactionReferencePort {

    @Override
    public String next() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
