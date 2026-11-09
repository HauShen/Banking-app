package com.Banking_app.transaction.adapter.in.web.mapper;

import com.Banking_app.shared.PageResult;
import com.Banking_app.transaction.adapter.in.web.dto.TransactionResponseBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferRequestBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferResponseBody;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.ForeignAmountView;
import com.Banking_app.transaction.application.port.in.result.TransactionView;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** HTTP DTOs ↔ use-case commands and results. Presentation choices (labels, messages) live here. */
@Component
public class TransactionWebMapper {

    static final String CASH_DEPOSIT_MACHINE = "Cash Deposit Machine";
    static final String TRANSFER_SUCCESSFUL = "Transfer successful";
    static final String DUPLICATE_REQUEST = "Duplicate request detected. Returning existing transaction.";

    public TransferMoneyCommand toCommand(TransferRequestBody body, String currentUserId) {
        return new TransferMoneyCommand(currentUserId, body.fromAccountNumber(), body.toAccountNumber(),
                body.amount(), body.currency(), body.description(), body.idempotencyKey());
    }

    public TransferResponseBody toResponse(TransferResult result) {
        ForeignAmountView original = result.original();
        return new TransferResponseBody(result.transactionReference(), result.status().name(), result.createdAt(),
                result.duplicate() ? DUPLICATE_REQUEST : TRANSFER_SUCCESSFUL,
                result.amount(),
                original == null ? null : original.currency(),
                original == null ? null : original.amount(),
                original == null ? null : original.exchangeRate());
    }

    public TransactionResponseBody toResponse(TransactionView view) {
        ForeignAmountView original = view.original();
        return new TransactionResponseBody(
                view.referenceNumber(),
                view.type().name(),
                view.fromAccountNumber() != null ? view.fromAccountNumber() : CASH_DEPOSIT_MACHINE,
                view.toAccountNumber(),
                view.amount(),
                view.currency(),
                original == null ? null : original.currency(),
                original == null ? null : original.amount(),
                original == null ? null : original.exchangeRate(),
                view.status().name(),
                view.description(),
                view.createdAt(),
                view.successAt());
    }

    /**
     * Keeps the JSON of the history endpoint exactly as before: the old code returned a Spring Data Page built from a
     * PageRequest sorted by createdAt DESC, so the frontend gets the same content / pageable / sort / totalPages ...
     * fields. (Spring Data logs a one-time warning about serializing PageImpl; it did that before too.)
     */
    public Page<TransactionResponseBody> toPage(PageResult<TransactionView> result) {
        PageRequest pageRequest = PageRequest.of(result.page(), result.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PageImpl<>(result.content().stream().map(this::toResponse).toList(), pageRequest,
                result.totalElements());
    }
}
