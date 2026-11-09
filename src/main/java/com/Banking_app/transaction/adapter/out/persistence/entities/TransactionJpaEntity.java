package com.Banking_app.transaction.adapter.out.persistence.entities;

import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.enums.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Row of the {@code transactions} table (V3 + V5). Persistence only: the rules live in the domain Transaction.
 *
 * The accounts are plain id columns, not {@code @ManyToOne AccountJpaEntity}: accounts belong to another module,
 * so this module never loads or joins account entities (same idea as AccountJpaEntity's plain user_id).
 * The foreign keys stay in the database.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reference_number", nullable = false, length = 100)
    private String referenceNumber;

    /** V9. The foreign-currency columns of V9 are mapped once the domain uses them (Phase 5A step 2). */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    /** Null for a cash deposit (top-up). */
    @Column(name = "from_account_id")
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "success_at")
    private Instant successAt;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    /**
     * Written together with the transaction (cascade). LAZY and never touched by the reads, so a page of history
     * is one query, not one extra query per row.
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerEntryJpaEntity> ledgerEntries = new ArrayList<>();

    /** Adds the entry and sets its back-reference, so transaction_id is filled in on insert. */
    public void addLedgerEntry(LedgerEntryJpaEntity entry) {
        entry.setTransaction(this);
        ledgerEntries.add(entry);
    }
}
