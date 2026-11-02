package com.Banking_app.controllers;

import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransactionService transactionService;
    @Autowired
    public TransferController(TransactionService transactionService){
        this.transactionService = transactionService;
    }
    @PostMapping
    public ResponseEntity<TransferResponseBody> transfer(
            @Valid @RequestBody TransferRequestBody transferRequestBody,
            @AuthenticationPrincipal(expression = "userId") String currentUserId) {
        // ResponseEntity.ok(...) used to override @ResponseStatus(CREATED), so this returned 200.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(transferRequestBody, currentUserId));
    }
    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponseBody> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(transactionService.getTransactionByReferenceNumber(reference));
    }
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionResponseBody>> getByAccountNumber(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountNumber(accountNumber, page, size));
    }


}
