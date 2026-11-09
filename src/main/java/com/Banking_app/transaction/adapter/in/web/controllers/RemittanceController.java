package com.Banking_app.transaction.adapter.in.web.controllers;

import com.Banking_app.transaction.adapter.in.web.dto.RemittanceRequestBody;
import com.Banking_app.transaction.adapter.in.web.dto.RemittanceResponseBody;
import com.Banking_app.transaction.adapter.in.web.mapper.TransactionWebMapper;
import com.Banking_app.transaction.application.port.in.usecase.RecordForeignRemittanceUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Money from abroad arrives: simulates the bank's payment gateway, so admins only (customers can't create incoming
 * money themselves). /api/admin/** is admin-only in SecurityConfig as well; @PreAuthorize keeps the rule next to the
 * code, like the other admin controllers.
 */
@RestController
@RequestMapping("/api/admin/remittances")
@PreAuthorize("hasRole('ADMIN')")
public class RemittanceController {

    private final RecordForeignRemittanceUseCase recordForeignRemittanceUseCase;
    private final TransactionWebMapper mapper;

    public RemittanceController(RecordForeignRemittanceUseCase recordForeignRemittanceUseCase,
                                TransactionWebMapper mapper) {
        this.recordForeignRemittanceUseCase = recordForeignRemittanceUseCase;
        this.mapper = mapper;
    }

    /** 201 for a new remittance and for a repeated idempotency key (then with the original), like transfers. */
    @PostMapping
    public ResponseEntity<RemittanceResponseBody> receive(@Valid @RequestBody RemittanceRequestBody body) {
        var result = recordForeignRemittanceUseCase.receive(mapper.toCommand(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }
}
