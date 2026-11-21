package com.Banking_app.alert.application.service;

import com.Banking_app.alert.application.port.in.command.CheckTransferCommand;
import com.Banking_app.alert.application.port.in.usecase.CheckTransferUseCase;
import com.Banking_app.alert.application.port.out.LoadAlertPort;
import com.Banking_app.alert.application.port.out.ProcessedEventsPort;
import com.Banking_app.alert.application.port.out.SaveAlertPort;
import com.Banking_app.alert.application.port.out.TransferLogPort;
import com.Banking_app.alert.domain.enums.AlertRule;
import com.Banking_app.alert.domain.models.AlertRules;
import com.Banking_app.alert.domain.models.ObservedTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Checks one transfer against the watch rules, in one database transaction:
 * <ol>
 *   <li>mark the event as processed by "alerts" — already there → a redelivery, stop</li>
 *   <li>remember the transfer in the module's transfer log, forget the account's entries older than the window</li>
 *   <li>large transfer? → alert</li>
 *   <li>more transfers in the window than allowed, and no rapid-transfers alert for this burst yet? → alert</li>
 * </ol>
 * All messages of one account arrive in order (they share the Kafka key, so the same partition), which is what makes
 * "the transfers in the last 10 minutes" a sound count.
 */
@Service
public class CheckTransferService implements CheckTransferUseCase {

    /** This consumer's name in processed_events; the notification consumer keeps its own marks. */
    static final String CONSUMER = "alerts";

    private static final Logger log = LoggerFactory.getLogger(CheckTransferService.class);

    private final AlertRules rules;
    private final ProcessedEventsPort processedEventsPort;
    private final TransferLogPort transferLogPort;
    private final LoadAlertPort loadAlertPort;
    private final SaveAlertPort saveAlertPort;
    private final Clock clock;

    public CheckTransferService(AlertRules rules, ProcessedEventsPort processedEventsPort,
                                TransferLogPort transferLogPort, LoadAlertPort loadAlertPort,
                                SaveAlertPort saveAlertPort, Clock clock) {
        this.rules = rules;
        this.processedEventsPort = processedEventsPort;
        this.transferLogPort = transferLogPort;
        this.loadAlertPort = loadAlertPort;
        this.saveAlertPort = saveAlertPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void check(CheckTransferCommand c) {
        if (!processedEventsPort.markProcessed(CONSUMER, c.eventId(), clock.instant())) {
            log.info("Alerts: event {} was already processed, skipping the redelivery", c.eventId());
            return;
        }
        ObservedTransfer transfer = new ObservedTransfer(c.eventId(), c.occurredAt(), c.reference(),
                c.fromAccountNumber(), c.fromOwnerId(), c.toAccountNumber(), c.amount());
        String account = transfer.fromAccountNumber();
        Instant windowStart = rules.windowStart(transfer.occurredAt());

        transferLogPort.record(transfer.eventId(), account, transfer.occurredAt());
        transferLogPort.forgetUpTo(account, windowStart);

        rules.checkLargeTransfer(transfer).ifPresent(saveAlertPort::save);

        long inWindow = transferLogPort.countInWindow(account, windowStart, transfer.occurredAt());
        boolean alreadyAlerted = loadAlertPort.existsAfter(AlertRule.RAPID_TRANSFERS, account, windowStart);
        rules.checkRapidTransfers(transfer, inWindow, alreadyAlerted).ifPresent(saveAlertPort::save);
    }
}
