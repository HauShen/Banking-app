package com.Banking_app.transaction.adapter.in.web.mapper;

import com.Banking_app.shared.PageResult;
import com.Banking_app.transaction.adapter.in.web.dto.TransactionResponseBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferRequestBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferResponseBody;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import com.Banking_app.transaction.domain.enums.TransactionStatus;
import com.Banking_app.transaction.domain.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionWebMapperTest {

    private static final Instant CREATED = Instant.parse("2026-11-06T08:00:00Z");
    private static final Instant SUCCEEDED = Instant.parse("2026-11-06T08:00:01Z");

    private final TransactionWebMapper mapper = new TransactionWebMapper();

    private static TransactionView view(String from) {
        TransactionType type = from == null ? TransactionType.CASH_DEPOSIT : TransactionType.TRANSFER;
        return new TransactionView("TXN-AAAABBBBCCCC", type, from, "1000000002", new BigDecimal("30.00"), "MYR",
                TransactionStatus.SUCCESS, "Dinner", CREATED, SUCCEEDED);
    }

    @Test
    @DisplayName("request body + logged-in user → command (currency is not passed on)")
    void toCommand() {
        TransferRequestBody body = new TransferRequestBody("1000000001", "1000000002", new BigDecimal("30.00"),
                "MYR", "Dinner", "key-1");

        TransferMoneyCommand command = mapper.toCommand(body, "alice");

        assertThat(command.requesterUserId()).isEqualTo("alice");
        assertThat(command.fromAccountNumber()).isEqualTo("1000000001");
        assertThat(command.toAccountNumber()).isEqualTo("1000000002");
        assertThat(command.amount()).isEqualByComparingTo("30.00");
        assertThat(command.description()).isEqualTo("Dinner");
        assertThat(command.idempotencyKey()).isEqualTo("key-1");
    }

    @Test
    @DisplayName("transfer result → reply with the old messages for new and duplicate requests")
    void transferResponse() {
        TransferResponseBody created = mapper.toResponse(
                new TransferResult("TXN-AAAABBBBCCCC", TransactionStatus.SUCCESS, CREATED, false));
        TransferResponseBody duplicate = mapper.toResponse(
                new TransferResult("TXN-AAAABBBBCCCC", TransactionStatus.SUCCESS, CREATED, true));

        assertThat(created.transactionReference()).isEqualTo("TXN-AAAABBBBCCCC");
        assertThat(created.status()).isEqualTo("SUCCESS");
        assertThat(created.createdAt()).isEqualTo(CREATED);
        assertThat(created.message()).isEqualTo("Transfer successful");
        assertThat(duplicate.message()).isEqualTo("Duplicate request detected. Returning existing transaction.");
    }

    @Test
    @DisplayName("transaction view → body; successAt becomes postedAt; a top-up shows 'Cash Deposit Machine'")
    void transactionResponse() {
        TransactionResponseBody transfer = mapper.toResponse(view("1000000001"));
        TransactionResponseBody topUp = mapper.toResponse(view(null));

        assertThat(transfer.reference()).isEqualTo("TXN-AAAABBBBCCCC");
        assertThat(transfer.fromAccountNumber()).isEqualTo("1000000001");
        assertThat(transfer.status()).isEqualTo("SUCCESS");
        assertThat(transfer.createdAt()).isEqualTo(CREATED);
        assertThat(transfer.postedAt()).isEqualTo(SUCCEEDED);
        assertThat(topUp.fromAccountNumber()).isEqualTo("Cash Deposit Machine");
    }

    @Test
    @DisplayName("page result → Spring Page with the same numbers, sorted (as the old PageRequest was)")
    void toPage() {
        Page<TransactionResponseBody> page = mapper.toPage(
                new PageResult<>(List.of(view("1000000001"), view(null)), 1, 2, 5));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(1).fromAccountNumber()).isEqualTo("Cash Deposit Machine");
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(5L);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getSort().isSorted()).isTrue();
    }
}
