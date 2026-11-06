package com.Banking_app.transaction.adapter.in.web.controllers;

import com.Banking_app.security.CurrentRequester;
import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.adapter.in.web.dto.TransactionResponseBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferRequestBody;
import com.Banking_app.transaction.adapter.in.web.dto.TransferResponseBody;
import com.Banking_app.transaction.adapter.in.web.mapper.TransactionWebMapper;
import com.Banking_app.transaction.application.port.in.query.ListAccountTransactionsQuery;
import com.Banking_app.transaction.application.port.in.usecase.GetTransactionUseCase;
import com.Banking_app.transaction.application.port.in.usecase.ListAccountTransactionsUseCase;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound web adapter: HTTP in, use case call, DTO out. No business rules here.
 * Same URLs, parameters, status codes and JSON as the old controllers.TransferController.
 *
 * Who may do what is decided in the use cases: the from-account's owner sends money; the sender, the recipient
 * and admins see a transaction; the owner and admins see an account's history.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferMoneyUseCase transferMoneyUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final ListAccountTransactionsUseCase listAccountTransactionsUseCase;
    private final TransactionWebMapper mapper;

    public TransferController(TransferMoneyUseCase transferMoneyUseCase,
                              GetTransactionUseCase getTransactionUseCase,
                              ListAccountTransactionsUseCase listAccountTransactionsUseCase,
                              TransactionWebMapper mapper) {
        this.transferMoneyUseCase = transferMoneyUseCase;
        this.getTransactionUseCase = getTransactionUseCase;
        this.listAccountTransactionsUseCase = listAccountTransactionsUseCase;
        this.mapper = mapper;
    }

    /** 201 for a new transfer and for a repeated idempotency key (then with the original transfer), as before. */
    @PostMapping
    public ResponseEntity<TransferResponseBody> transfer(
            @Valid @RequestBody TransferRequestBody body,
            @AuthenticationPrincipal(expression = "userId") String currentUserId) {
        var result = transferMoneyUseCase.transfer(mapper.toCommand(body, currentUserId));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponseBody> getByReference(
            @PathVariable String reference,
            @CurrentRequester Requester requester) {
        return ResponseEntity.ok(mapper.toResponse(getTransactionUseCase.getByReference(reference, requester)));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionResponseBody>> getByAccountNumber(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentRequester Requester requester) {
        var result = listAccountTransactionsUseCase.list(
                new ListAccountTransactionsQuery(accountNumber, page, size), requester);
        return ResponseEntity.ok(mapper.toPage(result));
    }
}
