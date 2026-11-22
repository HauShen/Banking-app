package com.Banking_app.alert.adapter.in.web.mapper;

import com.Banking_app.alert.adapter.in.web.dto.AlertResponseBody;
import com.Banking_app.alert.application.port.in.result.AlertView;
import com.Banking_app.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AlertWebMapper {

    public AlertResponseBody toResponse(AlertView view) {
        return new AlertResponseBody(view.id(), view.rule().name(), view.status().name(), view.accountNumber(),
                view.ownerId(), view.reference(), view.amount(), view.detail(), view.createdAt(), view.reviewedAt(),
                view.reviewedBy());
    }

    /** Spring Page JSON, like the other paged lists. */
    public Page<AlertResponseBody> toPage(PageResult<AlertView> result) {
        PageRequest pageRequest = PageRequest.of(result.page(), result.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PageImpl<>(result.content().stream().map(this::toResponse).toList(), pageRequest,
                result.totalElements());
    }
}
