package com.Banking_app.alert.application.service;

import com.Banking_app.alert.application.port.in.query.ListAlertsQuery;
import com.Banking_app.alert.application.port.in.result.AlertView;
import com.Banking_app.alert.application.port.in.usecase.ListAlertsUseCase;
import com.Banking_app.alert.application.port.out.LoadAlertPort;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListAlertsService implements ListAlertsUseCase {

    private final LoadAlertPort loadAlertPort;

    public ListAlertsService(LoadAlertPort loadAlertPort) {
        this.loadAlertPort = loadAlertPort;
    }

    @Override
    public PageResult<AlertView> list(ListAlertsQuery query) {
        return loadAlertPort.findPage(query.status(), query.page(), query.size()).map(AlertView::of);
    }

    @Override
    public long countOpen() {
        return loadAlertPort.countByStatus(AlertStatus.OPEN);
    }
}
