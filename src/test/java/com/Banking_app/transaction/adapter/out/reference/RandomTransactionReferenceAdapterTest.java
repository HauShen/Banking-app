package com.Banking_app.transaction.adapter.out.reference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RandomTransactionReferenceAdapterTest {

    private final RandomTransactionReferenceAdapter adapter = new RandomTransactionReferenceAdapter();

    @Test
    @DisplayName("same format as before: TXN- and 12 upper-case hex characters")
    void format() {
        assertThat(adapter.next()).matches("TXN-[0-9A-F]{12}");
    }

    @Test
    @DisplayName("1000 references in a row are all different")
    void unique() {
        Set<String> references = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            references.add(adapter.next());
        }
        assertThat(references).hasSize(1000);
    }
}
