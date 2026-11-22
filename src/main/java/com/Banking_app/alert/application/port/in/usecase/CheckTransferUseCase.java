package com.Banking_app.alert.application.port.in.usecase;

import com.Banking_app.alert.application.port.in.command.CheckTransferCommand;

/**
 * Runs the watch rules on one transfer and stores any alerts. Safe to call again with the same event (Kafka delivers
 * at least once): the second call changes nothing.
 */
public interface CheckTransferUseCase {

    void check(CheckTransferCommand command);
}
