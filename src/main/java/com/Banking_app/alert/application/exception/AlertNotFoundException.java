package com.Banking_app.alert.application.exception;

import com.Banking_app.exception.ResourceNotFoundException;

public class AlertNotFoundException extends ResourceNotFoundException {
    public AlertNotFoundException() {
        super("Alert not found");
    }
}
