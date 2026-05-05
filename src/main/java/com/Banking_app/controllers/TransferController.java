package com.Banking_app.controllers;

import com.Banking_app.dto.requestBodies.TransferRequestBody;
import com.Banking_app.dto.responseBodies.TransactionResponseBody;
import com.Banking_app.dto.responseBodies.TransferResponseBody;
import com.Banking_app.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransactionService transactionService;
    @Autowired
    public TransferController(TransactionService transactionService){
        this.transactionService = transactionService;
    }
    @PostMapping("/{transfer}")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponseBody transfer(@Valid @RequestBody TransferRequestBody transferRequestBody){
        return transactionService.transfer(transferRequestBody);
    }
    @GetMapping("/{get_reference}")
    public TransactionResponseBody getByReference(@PathVariable String reference){
        return transactionService.getTransactionByReferenceNumber(reference);
    }

}
