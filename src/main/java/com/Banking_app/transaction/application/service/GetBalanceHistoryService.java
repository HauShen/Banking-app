package com.Banking_app.transaction.application.service;

import com.Banking_app.shared.Requester;
import com.Banking_app.transaction.application.exception.TransactionAccessDeniedException;
import com.Banking_app.transaction.application.exception.TransactionAccountNotFoundException;
import com.Banking_app.transaction.application.port.in.query.BalanceHistoryQuery;
import com.Banking_app.transaction.application.port.in.result.BalanceHistoryView;
import com.Banking_app.transaction.application.port.in.usecase.GetBalanceHistoryUseCase;
import com.Banking_app.transaction.application.port.out.AccountDirectoryPort;
import com.Banking_app.transaction.application.port.out.AccountRef;
import com.Banking_app.transaction.application.port.out.LoadLedgerPort;
import com.Banking_app.transaction.domain.models.balance.DailyBalances;
import com.Banking_app.transaction.domain.models.transaction.LedgerEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Daily closing balances, worked out from the ledger on each request (nothing extra is stored). Days begin and end
 * in the bank's time zone ({@code bank.time-zone}, default Asia/Kuala_Lumpur), not in UTC, so "today" is the
 * customer's today.
 */
@Service
public class GetBalanceHistoryService implements GetBalanceHistoryUseCase {

    private final AccountDirectoryPort accountDirectoryPort;
    private final LoadLedgerPort loadLedgerPort;
    private final Clock clock;
    private final ZoneId zone;

    public GetBalanceHistoryService(AccountDirectoryPort accountDirectoryPort,
                                    LoadLedgerPort loadLedgerPort,
                                    Clock clock,
                                    @Value("${bank.time-zone:Asia/Kuala_Lumpur}") ZoneId zone) {
        this.accountDirectoryPort = accountDirectoryPort;
        this.loadLedgerPort = loadLedgerPort;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceHistoryView get(BalanceHistoryQuery query, Requester requester) {
        if (query.days() < 1 || query.days() > MAX_DAYS) {
            throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
        }
        AccountRef account = accountDirectoryPort.findByAccountNumber(query.accountNumber())
                .orElseThrow(() -> new TransactionAccountNotFoundException(query.accountNumber()));

        // Same rule as the transaction history: the owner or an admin.
        if (!requester.canAccessAccountsOf(account.ownerId())) {
            throw new TransactionAccessDeniedException("You can only view the balance history of your own accounts");
        }

        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate openedOn = LocalDate.ofInstant(account.openedAt(), zone);
        LocalDate periodStart = today.minusDays(query.days() - 1L);
        LocalDate firstDay = openedOn.isAfter(periodStart) ? openedOn : periodStart;   // no days before opening
        Instant firstDayBegins = firstDay.atStartOfDay(zone).toInstant();

        List<LedgerEntry> entries = loadLedgerPort.findPostedSince(account.accountId(), firstDayBegins);
        BigDecimal balanceAtStart = loadLedgerPort.findLastPostedBefore(account.accountId(), firstDayBegins)
                .map(LedgerEntry::balanceAfter)
                // Nothing posted before the period: the first entry in it starts from that balance; with no
                // entries at all, the balance has not changed since the account was opened.
                .orElseGet(() -> entries.isEmpty() ? account.balance() : entries.get(0).balanceBefore());

        return new BalanceHistoryView(account.accountNumber(), account.currency(), zone.getId(),
                DailyBalances.closingBalances(balanceAtStart, entries, firstDay, today, zone));
    }
}
