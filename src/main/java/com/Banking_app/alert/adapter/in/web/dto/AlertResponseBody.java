package com.Banking_app.alert.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One alert in GET /api/admin/alerts. {@code createdAt} is when the transfer happened; {@code amount} is MYR. */
public record AlertResponseBody(long id,
                                String rule,
                                String status,
                                String accountNumber,
                                String ownerId,
                                String reference,
                                BigDecimal amount,
                                String detail,
                                Instant createdAt,
                                Instant reviewedAt,
                                String reviewedBy) {
}
