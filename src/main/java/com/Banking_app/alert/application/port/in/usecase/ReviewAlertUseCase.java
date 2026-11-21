package com.Banking_app.alert.application.port.in.usecase;

import com.Banking_app.alert.application.port.in.result.AlertView;

public interface ReviewAlertUseCase {

    /** Marks the alert as reviewed by {@code adminId}; already reviewed → unchanged. Unknown → 404. */
    AlertView review(long alertId, String adminId);
}
