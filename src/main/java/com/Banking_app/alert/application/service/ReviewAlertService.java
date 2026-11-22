package com.Banking_app.alert.application.service;

import com.Banking_app.alert.application.exception.AlertNotFoundException;
import com.Banking_app.alert.application.port.in.result.AlertView;
import com.Banking_app.alert.application.port.in.usecase.ReviewAlertUseCase;
import com.Banking_app.alert.application.port.out.LoadAlertPort;
import com.Banking_app.alert.application.port.out.SaveAlertPort;
import com.Banking_app.alert.domain.models.TransactionAlert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@Transactional
public class ReviewAlertService implements ReviewAlertUseCase {

    private final LoadAlertPort loadAlertPort;
    private final SaveAlertPort saveAlertPort;
    private final Clock clock;

    public ReviewAlertService(LoadAlertPort loadAlertPort, SaveAlertPort saveAlertPort, Clock clock) {
        this.loadAlertPort = loadAlertPort;
        this.saveAlertPort = saveAlertPort;
        this.clock = clock;
    }

    @Override
    public AlertView review(long alertId, String adminId) {
        TransactionAlert alert = loadAlertPort.findById(alertId).orElseThrow(AlertNotFoundException::new);
        if (alert.isReviewed()) {
            return AlertView.of(alert);
        }
        return AlertView.of(saveAlertPort.save(alert.review(adminId, clock.instant())));
    }
}
