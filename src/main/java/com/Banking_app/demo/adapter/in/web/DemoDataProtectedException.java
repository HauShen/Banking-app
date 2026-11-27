package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.exception.ForbiddenException;

/**
 * A visitor tried to change a seeded demo user or account (plan step C1). A {@link ForbiddenException}, so
 * GlobalExceptionHandler answers 403 with this message in the usual error JSON, which the frontend shows as is.
 */
public class DemoDataProtectedException extends ForbiddenException {

    public DemoDataProtectedException(String message) {
        super(message);
    }
}
