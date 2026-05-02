package com.Banking_app.controllers;

import com.Banking_app.dto.requestBodies.AccountRequestBody;
import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.models.enums.AccountStatus;
import com.Banking_app.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;
    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @PostMapping("/create")
    public ResponseEntity<AccountResponseBody>createAccount(@Valid @RequestBody AccountRequestBody request){
        AccountResponseBody created = accountService.createAccountWithUserId(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    @GetMapping("/get_all")
    public ResponseEntity<List<AccountResponseBody>>getAllAccounts(@RequestParam(required = false) AccountStatus status){
        if (status != null) {
            return ResponseEntity.ok(accountService.getAccountsByStatus(status));
        }
        return ResponseEntity.ok(accountService.getAllAccounts());
    }
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseBody>getAccountById(@PathVariable Long accountId){
        return ResponseEntity.ok(accountService.getAccountByAccountId(accountId));
    }
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponseBody>getByAccountNumber(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getByAccountNumber(accountNumber));
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponseBody>updateStatus(@PathVariable Long id, @RequestParam AccountStatus status){
        return ResponseEntity.ok(accountService.updateAccountStatus(id, status));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
