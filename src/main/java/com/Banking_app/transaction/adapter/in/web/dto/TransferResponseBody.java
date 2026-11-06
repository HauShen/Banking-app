package com.Banking_app.transaction.adapter.in.web.dto;

import java.time.Instant;

/** Reply to POST /api/transfers. Same JSON fields as before. */
public record TransferResponseBody(String transactionReference,
                                   String status,
                                   Instant createdAt,
                                   String message) {
}
