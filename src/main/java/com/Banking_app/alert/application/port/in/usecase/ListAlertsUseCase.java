package com.Banking_app.alert.application.port.in.usecase;

import com.Banking_app.alert.application.port.in.query.ListAlertsQuery;
import com.Banking_app.alert.application.port.in.result.AlertView;
import com.Banking_app.shared.PageResult;

/** For admins (the web adapter only exposes it under /api/admin). */
public interface ListAlertsUseCase {

    PageResult<AlertView> list(ListAlertsQuery query);

    long countOpen();
}
