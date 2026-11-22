package com.Banking_app.notification.domain.models;

import com.Banking_app.notification.domain.enums.NotificationType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A message for one user about money that moved in one of their accounts, e.g. "You received RM 30.00 from account
 * ****0001." Created from an event; the user can only mark it as read.
 *
 * <p>Account numbers are masked to their last 4 digits: a notification is shown in a header dropdown, maybe on a
 * shared screen, so it shows no more than a bank's push message would.
 *
 * <p>Immutable: {@link #markRead(Instant)} returns a copy.
 */
public final class Notification {

    private final Long id;              // null until saved
    private final UUID eventId;         // the event it was created from
    private final String userId;
    private final NotificationType type;
    private final String message;
    private final String reference;     // the transaction's reference number
    private final BigDecimal amount;    // MYR
    private final Instant createdAt;    // when the money moved
    private final Instant readAt;       // null = unread

    private Notification(Long id, UUID eventId, String userId, NotificationType type, String message,
                         String reference, BigDecimal amount, Instant createdAt, Instant readAt) {
        this.id = id;
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.type = Objects.requireNonNull(type, "type");
        this.message = Objects.requireNonNull(message, "message");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.readAt = readAt;
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A notification's amount must be positive");
        }
    }

    // ------------------------------------------------------------------ creation from events

    /** For the sender of a transfer: "You sent RM 95.00 (USD 20.00) to account ****0002." */
    public static Notification moneySent(UUID eventId, String userId, String reference, BigDecimal amount,
                                         ForeignAmount original, String toAccountNumber, Instant at) {
        String message = "You sent " + rm(amount) + inBrackets(original) + " to account " + mask(toAccountNumber) + ".";
        return new Notification(null, eventId, userId, NotificationType.MONEY_SENT, message, reference, amount, at, null);
    }

    /** For the recipient of a transfer: "You received RM 30.00 from account ****0001." */
    public static Notification moneyReceived(UUID eventId, String userId, String reference, BigDecimal amount,
                                             String fromAccountNumber, Instant at) {
        String message = "You received " + rm(amount) + " from account " + mask(fromAccountNumber) + ".";
        return new Notification(null, eventId, userId, NotificationType.MONEY_RECEIVED, message, reference, amount,
                at, null);
    }

    /** "RM 50.00 was deposited into your account ****0001." */
    public static Notification cashDeposited(UUID eventId, String userId, String reference, BigDecimal amount,
                                             String accountNumber, Instant at) {
        String message = rm(amount) + " was deposited into your account " + mask(accountNumber) + ".";
        return new Notification(null, eventId, userId, NotificationType.CASH_DEPOSITED, message, reference, amount,
                at, null);
    }

    /** "You received RM 460.00 (USD 100.00) from abroad in account ****0002." */
    public static Notification remittanceReceived(UUID eventId, String userId, String reference, BigDecimal amount,
                                                  ForeignAmount original, String accountNumber, Instant at) {
        String message = "You received " + rm(amount) + inBrackets(original) + " from abroad in account "
                + mask(accountNumber) + ".";
        return new Notification(null, eventId, userId, NotificationType.REMITTANCE_RECEIVED, message, reference,
                amount, at, null);
    }

    /** Rebuilds a stored notification (persistence adapter). */
    public static Notification rehydrate(Long id, UUID eventId, String userId, NotificationType type, String message,
                                         String reference, BigDecimal amount, Instant createdAt, Instant readAt) {
        return new Notification(id, eventId, userId, type, message, reference, amount, createdAt, readAt);
    }

    // ------------------------------------------------------------------ behaviour

    /** A read copy. Marking an already read notification again keeps the first read time. */
    public Notification markRead(Instant now) {
        if (isRead()) {
            return this;
        }
        return new Notification(id, eventId, userId, type, message, reference, amount, createdAt,
                Objects.requireNonNull(now, "now"));
    }

    public boolean isRead() {
        return readAt != null;
    }

    public boolean belongsTo(String someUserId) {
        return userId.equals(someUserId);
    }

    // ------------------------------------------------------------------ text

    static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return String.valueOf(accountNumber);
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private static String rm(BigDecimal amount) {
        return "RM " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String inBrackets(ForeignAmount original) {
        return original == null ? "" : " (" + original + ")";
    }

    // ------------------------------------------------------------------ getters

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
