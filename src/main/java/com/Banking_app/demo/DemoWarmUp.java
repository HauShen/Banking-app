package com.Banking_app.demo;

import com.Banking_app.transaction.application.port.in.command.RecordForeignRemittanceCommand;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.usecase.RecordForeignRemittanceUseCase;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.Banking_app.demo.DemoCast.AHMAD_CURRENT;
import static com.Banking_app.demo.DemoCast.MAIN_SAVINGS;
import static com.Banking_app.demo.DemoCast.MEI_LING_SAVINGS;
import static com.Banking_app.demo.DemoCast.PRIYA_SAVINGS;

/**
 * Four real transactions right after the history is written (plan step 5b), so the notification bell and the admin
 * Alerts page aren't empty when the first visitor arrives.
 *
 * <p>Unlike the history, these go through the transaction module's inbound ports, exactly like a request from the
 * web: ownership and lock checks, ledger rows, and an outbox event that the relay sends to Kafka, where the alert
 * module and the notification-service pick it up. Each call runs in its own database transaction (the use cases are
 * {@code @Transactional}). If Kafka isn't reachable yet, the events wait in the outbox.
 *
 * <p>The last transfer is RM 10,500 out of ahmad.f's current account: above the RM 10,000 limit, so it raises a
 * LARGE_TRANSFER alert. The story generator keeps that account at RM 11,000 or more, so it can always pay.
 *
 * <p>Idempotency keys are fixed UUIDs (the web API only accepts UUIDs for transfers). They repeat after every reset,
 * which is fine because the reset empties the tables first.
 */
public class DemoWarmUp {

    static final BigDecimal LARGE_TRANSFER = new BigDecimal("10500.00");

    private final TransferMoneyUseCase transfers;
    private final RecordForeignRemittanceUseCase remittances;

    public DemoWarmUp(TransferMoneyUseCase transfers, RecordForeignRemittanceUseCase remittances) {
        this.transfers = Objects.requireNonNull(transfers, "transfers");
        this.remittances = Objects.requireNonNull(remittances, "remittances");
    }

    /** @return the references of the four transactions, oldest first */
    public List<String> run(DemoCast cast) {
        String dinner = transfer(cast, PRIYA_SAVINGS, MAIN_SAVINGS, new BigDecimal("45.00"), "Dinner split", 1);
        String grab = transfer(cast, MAIN_SAVINGS, MEI_LING_SAVINGS, new BigDecimal("30.00"), "Grab ride share", 2);
        String remittance = remittances.receive(new RecordForeignRemittanceCommand(
                MAIN_SAVINGS, "USD", new BigDecimal("100.00"), "Lee Chong", idempotencyKey(3)))
                .transactionReference();
        String large = transfer(cast, AHMAD_CURRENT, PRIYA_SAVINGS, LARGE_TRANSFER, "Car down payment", 4);
        return List.of(dinner, grab, remittance, large);
    }

    /** A transfer requested by the sending account's owner, so the use case's ownership check passes. */
    private String transfer(DemoCast cast, String from, String to, BigDecimal amount, String description, int n) {
        String requester = cast.user(cast.account(from).ownerUsername()).id();
        return transfers.transfer(new TransferMoneyCommand(requester, from, to, amount, TransferMoneyCommand.MYR,
                description, idempotencyKey(n), null)).transactionReference();
    }

    /** A fixed UUID per warm-up transaction: the format the web API accepts for transfers. */
    static String idempotencyKey(int n) {
        return UUID.nameUUIDFromBytes(("banking-demo-warm-up:" + n).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
