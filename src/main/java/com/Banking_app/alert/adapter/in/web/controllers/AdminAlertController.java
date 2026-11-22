package com.Banking_app.alert.adapter.in.web.controllers;

import com.Banking_app.alert.adapter.in.web.dto.AlertResponseBody;
import com.Banking_app.alert.adapter.in.web.dto.OpenAlertCountResponseBody;
import com.Banking_app.alert.adapter.in.web.mapper.AlertWebMapper;
import com.Banking_app.alert.application.port.in.query.ListAlertsQuery;
import com.Banking_app.alert.application.port.in.usecase.ListAlertsUseCase;
import com.Banking_app.alert.application.port.in.usecase.ReviewAlertUseCase;
import com.Banking_app.alert.domain.enums.AlertStatus;
import com.Banking_app.security.CurrentRequester;
import com.Banking_app.shared.Requester;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction alerts for admins. /api/admin/** is admin-only in SecurityConfig as well; @PreAuthorize keeps the rule
 * next to the code, like the other admin controllers.
 */
@RestController
@RequestMapping("/api/admin/alerts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlertController {

    private final ListAlertsUseCase listAlertsUseCase;
    private final ReviewAlertUseCase reviewAlertUseCase;
    private final AlertWebMapper mapper;

    public AdminAlertController(ListAlertsUseCase listAlertsUseCase, ReviewAlertUseCase reviewAlertUseCase,
                                AlertWebMapper mapper) {
        this.listAlertsUseCase = listAlertsUseCase;
        this.reviewAlertUseCase = reviewAlertUseCase;
        this.mapper = mapper;
    }

    /** Newest first; {@code status} OPEN or REVIEWED, absent = all; {@code size} 1..100 (default 20). */
    @GetMapping
    public ResponseEntity<Page<AlertResponseBody>> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mapper.toPage(listAlertsUseCase.list(new ListAlertsQuery(status, page, size))));
    }

    @GetMapping("/open-count")
    public ResponseEntity<OpenAlertCountResponseBody> openCount() {
        return ResponseEntity.ok(new OpenAlertCountResponseBody(listAlertsUseCase.countOpen()));
    }

    /** Marks the alert as reviewed by the calling admin; already reviewed → unchanged (200 either way). */
    @PatchMapping("/{id}/review")
    public ResponseEntity<AlertResponseBody> review(@PathVariable long id, @CurrentRequester Requester requester) {
        return ResponseEntity.ok(mapper.toResponse(reviewAlertUseCase.review(id, requester.userId())));
    }
}
