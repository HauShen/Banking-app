package com.Banking_app.account.adapter.in.web.controllers;

import com.Banking_app.account.adapter.in.web.dto.AccountRequestBody;
import com.Banking_app.account.adapter.in.web.dto.AccountResponseBody;
import com.Banking_app.account.adapter.in.web.dto.AccountSummaryResponseBody;
import com.Banking_app.account.adapter.in.web.mapper.AccountWebMapper;
import com.Banking_app.account.application.port.in.command.ChangeAccountStatusCommand;
import com.Banking_app.account.application.port.in.command.DeleteAccountCommand;
import com.Banking_app.account.application.port.in.command.OpenAccountCommand;
import com.Banking_app.account.application.port.in.command.TopUpAccountCommand;
import com.Banking_app.account.application.port.in.query.ListAccountsQuery;
import com.Banking_app.account.application.port.in.usecase.ChangeAccountStatusUseCase;
import com.Banking_app.account.application.port.in.usecase.DeleteAccountUseCase;
import com.Banking_app.account.application.port.in.usecase.GetAccountSummaryUseCase;
import com.Banking_app.account.application.port.in.usecase.GetAccountUseCase;
import com.Banking_app.account.application.port.in.usecase.ListAccountsUseCase;
import com.Banking_app.account.application.port.in.usecase.OpenAccountUseCase;
import com.Banking_app.account.application.port.in.usecase.TopUpAccountUseCase;
import com.Banking_app.account.domain.enums.AccountStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inbound web adapter: HTTP in, use case call, DTO out. No business rules here.
 * Same URLs, parameters and JSON as the old controllers.AccountController.
 *
 * The logged-in user's id comes from @AuthenticationPrincipal(expression = "userId"):
 * Spring reads getUserId() from the principal (SecurityUserDetails) without this module
 * importing the userProfile security class. This also fixes the old bug where the parameter
 * was typed UserProfileJpaEntity, resolved to null, and /summary and /topup returned 500.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final OpenAccountUseCase openAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final GetAccountSummaryUseCase getAccountSummaryUseCase;
    private final ChangeAccountStatusUseCase changeAccountStatusUseCase;
    private final TopUpAccountUseCase topUpAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final AccountWebMapper mapper;

    public AccountController(OpenAccountUseCase openAccountUseCase,
                             GetAccountUseCase getAccountUseCase,
                             ListAccountsUseCase listAccountsUseCase,
                             GetAccountSummaryUseCase getAccountSummaryUseCase,
                             ChangeAccountStatusUseCase changeAccountStatusUseCase,
                             TopUpAccountUseCase topUpAccountUseCase,
                             DeleteAccountUseCase deleteAccountUseCase,
                             AccountWebMapper mapper) {
        this.openAccountUseCase = openAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.listAccountsUseCase = listAccountsUseCase;
        this.getAccountSummaryUseCase = getAccountSummaryUseCase;
        this.changeAccountStatusUseCase = changeAccountStatusUseCase;
        this.topUpAccountUseCase = topUpAccountUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/summary")
    public ResponseEntity<AccountSummaryResponseBody> getSummary(
            @AuthenticationPrincipal(expression = "userId") String currentUserId) {
        return ResponseEntity.ok(mapper.toSummaryResponse(getAccountSummaryUseCase.getSummary(currentUserId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<AccountResponseBody> createAccountByUserId(@Valid @RequestBody AccountRequestBody request) {
        var account = openAccountUseCase.open(new OpenAccountCommand(request.userId(), request.accountType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(account));
    }

    @PatchMapping("/{id}/topup")
    public ResponseEntity<AccountResponseBody> topUp(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal(expression = "userId") String currentUserId) {
        var account = topUpAccountUseCase.topUp(new TopUpAccountCommand(id, amount, currentUserId));
        return ResponseEntity.ok(mapper.toResponse(account));
    }

    @GetMapping("/get_all")
    public ResponseEntity<List<AccountResponseBody>> getAllAccounts(
            @RequestParam(required = false) AccountStatus status) {
        return ResponseEntity.ok(mapper.toResponses(listAccountsUseCase.list(new ListAccountsQuery(status))));
    }

    @GetMapping("/get_all/{user_id}")
    public ResponseEntity<List<AccountResponseBody>> getAllAccountsByUserId(@PathVariable("user_id") String userId) {
        return ResponseEntity.ok(mapper.toResponses(listAccountsUseCase.listByOwner(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseBody> getAccountById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mapper.toResponse(getAccountUseCase.getById(id)));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponseBody> getByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(mapper.toResponse(getAccountUseCase.getByAccountNumber(accountNumber)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponseBody> updateStatus(@PathVariable Long id, @RequestParam AccountStatus status) {
        var account = changeAccountStatusUseCase.changeStatus(new ChangeAccountStatusCommand(id, status));
        return ResponseEntity.ok(mapper.toResponse(account));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        deleteAccountUseCase.delete(new DeleteAccountCommand(id));
        return ResponseEntity.noContent().build();
    }
}
