package com.Banking_app.controllers;

import com.Banking_app.dto.requestBodies.AccountRequestBody;
import com.Banking_app.dto.responseBodies.AccountResponseBody;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.enums.AccountStatus;
import com.Banking_app.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;
    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@AuthenticationPrincipal UserProfileJpaEntity user) {
        List<AccountResponseBody> accounts = accountService.getAllAccountsByUserId(user.getId());
        double totalBalance = accounts.stream()
                .mapToDouble(a -> a.getBalance() != null ? a.getBalance().doubleValue() : 0)
                .sum();
        String accountType = accounts.isEmpty() ? "N/A" : accounts.get(0).getAccountType().name();
        return ResponseEntity.ok(Map.of(
                "balance",     totalBalance,
                "accountType", accountType,
                "accounts",    accounts
        ));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<AccountResponseBody> createAccountByUserId(@Valid @RequestBody AccountRequestBody request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccountWithUserId(request));
    }
    @PatchMapping("/{id}/topup")
    public ResponseEntity<AccountResponseBody> topUp(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserProfileJpaEntity user) {
        return ResponseEntity.ok(accountService.topUp(id, amount, user.getId()));
    }

    @GetMapping("/get_all")
    public ResponseEntity<List<AccountResponseBody>>getAllAccounts(@RequestParam(required = false) AccountStatus status){
        if (status != null) {
            return ResponseEntity.ok(accountService.getAccountsByStatus(status));
        }
        return ResponseEntity.ok(accountService.getAllAccounts());
    }
    @GetMapping("/get_all/{user_id}")
    public ResponseEntity<List<AccountResponseBody>> getAllAccountsByUserId(@PathVariable("user_id") String userId){
        return ResponseEntity.ok(accountService.getAllAccountsByUserId(userId));
    }
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseBody>getAccountById(@PathVariable("id") Long id){
        return ResponseEntity.ok(accountService.getAccountByAccountId(id));
    }
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponseBody>getByAccountNumber(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getByAccountNumber(accountNumber));
    }
    @PreAuthorize("hasRole('ADMIN')") // Only for Admin use.
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponseBody>updateStatus(@PathVariable Long id, @RequestParam AccountStatus status){
        return ResponseEntity.ok(accountService.updateAccountStatus(id, status));
    }
    @PreAuthorize("hasRole('ADMIN')") // Only for Admin use.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
