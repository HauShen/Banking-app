package com.Banking_app.transaction.adapter.in.web.mapper;

import com.Banking_app.shared.PageResult;
import com.Banking_app.transaction.adapter.in.web.dto.RemittanceRequestBody;
import com.Banking_app.transaction.adapter.in.web.dto.RemittanceResponseBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransactionResponseBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferRequestBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferResponseBody;
import com.Banking_app.transaction.application.port.in.command.RecordForeignRemittanceCommand;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.ForeignAmountView;
import com.Banking_app.transaction.application.port.in.result.RemittanceResult;
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
                null, TransactionStatus.SUCCESS, "Dinner", CREATED, SUCCEEDED);
    }

    private static final ForeignAmountView USD_20 =
            new ForeignAmountView("USD", new BigDecimal("20.00"), new BigDecimal("4.75"));

    @Test
    @DisplayName("request body + logged-in user → command, with the currency of the amount")
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
        assertThat(command.currency()).isEqualTo("MYR");
    }

    @Test
    @DisplayName("currency is passed on; absent means MYR")
    void currencyOnCommand() {
        TransferRequestBody usd = new TransferRequestBody("1000000001", "1000000002", new BigDecimal("20.00"),
                "USD", "Dinner", "key-1");
        TransferRequestBody none = new TransferRequestBody("1000000001", "1000000002", new BigDecimal("20.00"),
                null, "Dinner", "key-1");

        assertThat(mapper.toCommand(usd, "alice").currency()).isEqualTo("USD");
        assertThat(mapper.toCommand(usd, "alice").isForeignCurrency()).isTrue();
        assertThat(mapper.toCommand(none, "alice").currency()).isEqualTo("MYR");
        assertThat(mapper.toCommand(none, "alice").isForeignCurrency()).isFalse();
    }

    @Test
    @DisplayName("transfer result → reply with the old messages for new and duplicate requests")
    void transferResponse() {
        TransferResponseBody created = mapper.toResponse(new TransferResult("TXN-AAAABBBBCCCC",
                TransactionStatus.SUCCESS, CREATED, new BigDecimal("30.00"), null, false));
        TransferResponseBody duplicate = mapper.toResponse(new TransferResult("TXN-AAAABBBBCCCC",
                TransactionStatus.SUCCESS, CREATED, new BigDecimal("30.00"), null, true));

        assertThat(created.transactionReference()).isEqualTo("TXN-AAAABBBBCCCC");
        assertThat(created.status()).isEqualTo("SUCCESS");
        assertThat(created.createdAt()).isEqualTo(CREATED);
        assertThat(created.message()).isEqualTo("Transfer successful");
        assertThat(duplicate.message()).isEqualTo("Duplicate request detected. Returning existing transaction.");
        assertThat(created.amount()).isEqualByComparingTo("30.00");
        assertThat(created.originalCurrency()).isNull();
        assertThat(created.originalAmount()).isNull();
        assertThat(created.exchangeRate()).isNull();
    }

    @Test
    @DisplayName("a transfer paid in USD → reply with the MYR amount plus original currency, amount and rate")
    void foreignTransferResponse() {
        TransferResponseBody body = mapper.toResponse(new TransferResult("TXN-AAAABBBBCCCC",
                TransactionStatus.SUCCESS, CREATED, new BigDecimal("95.00"), USD_20, false));

        assertThat(body.amount()).isEqualByComparingTo("95.00");
        assertThat(body.originalCurrency()).isEqualTo("USD");
        assertThat(body.originalAmount()).isEqualByComparingTo("20.00");
        assertThat(body.exchangeRate()).isEqualByComparingTo("4.75");
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
        assertThat(transfer.type()).isEqualTo("TRANSFER");
        assertThat(topUp.type()).isEqualTo("CASH_DEPOSIT");
        assertThat(transfer.originalCurrency()).isNull();
        assertThat(transfer.originalAmount()).isNull();
        assertThat(transfer.exchangeRate()).isNull();
    }

    @Test
    @DisplayName("a foreign-currency transaction view → body with original currency, amount and rate")
    void foreignTransactionResponse() {
        TransactionView view = new TransactionView("TXN-AAAABBBBCCCC", TransactionType.TRANSFER, "1000000001",
                "1000000002", new BigDecimal("95.00"), "MYR", USD_20, TransactionStatus.SUCCESS, "Dinner", CREATED,
                SUCCEEDED);

        TransactionResponseBody body = mapper.toResponse(view);

        assertThat(body.amount()).isEqualByComparingTo("95.00");
        assertThat(body.currency()).isEqualTo("MYR");
        assertThat(body.originalCurrency()).isEqualTo("USD");
        assertThat(body.originalAmount()).isEqualByComparingTo("20.00");
        assertThat(body.exchangeRate()).isEqualByComparingTo("4.75");
    }

    @Test
    @DisplayName("a foreign remittance has no from-account either: the history shows 'Foreign Remittance'")
    void remittanceLabel() {
        TransactionView remittance = new TransactionView("RMT-AAAABBBBCCCC", TransactionType.FOREIGN_REMITTANCE, null,
                "1000000002", new BigDecimal("460.00"), "MYR",
                new ForeignAmountView("USD", new BigDecimal("100.00"), new BigDecimal("4.60")),
                TransactionStatus.SUCCESS, "Foreign remittance from John Smith", CREATED, SUCCEEDED);

        TransactionResponseBody body = mapper.toResponse(remittance);

        assertThat(body.fromAccountNumber()).isEqualTo("Foreign Remittance");
        assertThat(body.type()).isEqualTo("FOREIGN_REMITTANCE");
        assertThat(body.originalCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("remittance body → command; result → reply with 'Remittance received' or the duplicate message")
    void remittance() {
        RecordForeignRemittanceCommand command = mapper.toCommand(new RemittanceRequestBody("1000000002", "USD",
                new BigDecimal("100.00"), "John Smith", "gw-1"));
        assertThat(command.toAccountNumber()).isEqualTo("1000000002");
        assertThat(command.currency()).isEqualTo("USD");
        assertThat(command.amount()).isEqualByComparingTo("100.00");
        assertThat(command.senderName()).isEqualTo("John Smith");
        assertThat(command.idempotencyKey()).isEqualTo("gw-1");

        ForeignAmountView usd100 = new ForeignAmountView("USD", new BigDecimal("100.00"), new BigDecimal("4.60"));
        RemittanceResponseBody created = mapper.toResponse(new RemittanceResult("RMT-AAAABBBBCCCC",
                TransactionStatus.SUCCESS, CREATED, "1000000002", new BigDecimal("460.00"), usd100, false));
        RemittanceResponseBody duplicate = mapper.toResponse(new RemittanceResult("RMT-AAAABBBBCCCC",
                TransactionStatus.SUCCESS, CREATED, "1000000002", new BigDecimal("460.00"), usd100, true));

        assertThat(created.transactionReference()).isEqualTo("RMT-AAAABBBBCCCC");
        assertThat(created.status()).isEqualTo("SUCCESS");
        assertThat(created.message()).isEqualTo("Remittance received");
        assertThat(created.toAccountNumber()).isEqualTo("1000000002");
        assertThat(created.amount()).isEqualByComparingTo("460.00");
        assertThat(created.originalCurrency()).isEqualTo("USD");
        assertThat(created.originalAmount()).isEqualByComparingTo("100.00");
        assertThat(created.exchangeRate()).isEqualByComparingTo("4.60");
        assertThat(duplicate.message()).isEqualTo("Duplicate request detected. Returning existing transaction.");
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
