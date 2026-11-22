package com.Banking_app.notification.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One notification in GET /api/notifications. {@code amount} is MYR; {@code createdAt} is when the money moved. */
public record NotificationResponseBody(long id,
                                       String type,
                                       String message,
                                       String reference,
                                       BigDecimal amount,
                                       Instant createdAt,
                                       boolean read) {
}
