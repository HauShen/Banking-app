package com.Banking_app.alert.domain.models;

import com.Banking_app.alert.domain.enums.AlertRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The two watch rules, with their limits (from configuration, app.alerts.*):
 * <ul>
 *   <li><b>Large transfer:</b> one transfer of at least {@code largeAmount} MYR.</li>
 *   <li><b>Rapid transfers:</b> more than {@code maxTransfers} transfers out of one account within {@code window}.
 *       One alert per burst: while the account already has a rapid-transfers alert inside the window, the next
 *       transfers of the same burst raise no new one.</li>
 * </ul>
 * Rules for demo and portfolio purposes; a real bank's transaction monitoring is far more involved.
 */
public final class AlertRules {

    private final BigDecimal largeAmount;
    private final int maxTransfers;
    private final Duration window;

    public AlertRules(BigDecimal largeAmount, int maxTransfers, Duration window) {
        this.largeAmount = Objects.requireNonNull(largeAmount, "largeAmount");
        this.maxTransfers = maxTransfers;
        this.window = Objects.requireNonNull(window, "window");
        if (largeAmount.signum() <= 0 || maxTransfers < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Alert limits must be positive");
        }
    }

    /** The window that ends at {@code at}: transfers after this moment (and up to {@code at}) count. */
    public Instant windowStart(Instant at) {
        return at.minus(window);
    }

    public Optional<TransactionAlert> checkLargeTransfer(ObservedTransfer transfer) {
        if (transfer.amount().compareTo(largeAmount) < 0) {
            return Optional.empty();
        }
        return Optional.of(TransactionAlert.raise(AlertRule.LARGE_TRANSFER, transfer,
                "Transfer of " + rm(transfer.amount()) + " is at or above the " + rm(largeAmount) + " limit"));
    }

    /**
     * @param transfersInWindow       the account's outgoing transfers in the window, this one included
     * @param alreadyAlertedInWindow  the account already has a rapid-transfers alert inside the window
     */
    public Optional<TransactionAlert> checkRapidTransfers(ObservedTransfer transfer, long transfersInWindow,
                                                          boolean alreadyAlertedInWindow) {
        if (transfersInWindow <= maxTransfers || alreadyAlertedInWindow) {
            return Optional.empty();
        }
        return Optional.of(TransactionAlert.raise(AlertRule.RAPID_TRANSFERS, transfer,
                transfersInWindow + " transfers from this account within " + describe(window)
                        + " (limit " + maxTransfers + ")"));
    }

    private static String rm(BigDecimal amount) {
        return "RM " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 10m → "10 minutes", 1m → "1 minute", 90s → "90 seconds". */
    static String describe(Duration duration) {
        if (duration.toSecondsPart() == 0 && duration.toMinutes() > 0) {
            long minutes = duration.toMinutes();
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        long seconds = duration.toSeconds();
        return seconds + (seconds == 1 ? " second" : " seconds");
    }
}
