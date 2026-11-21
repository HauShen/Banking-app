package com.Banking_app.notification.adapter.in.web.mapper;

import com.Banking_app.notification.adapter.in.web.dto.NotificationResponseBody;
import com.Banking_app.notification.application.port.in.result.NotificationView;
import com.Banking_app.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class NotificationWebMapper {

    public NotificationResponseBody toResponse(NotificationView view) {
        return new NotificationResponseBody(view.id(), view.type().name(), view.message(), view.reference(),
                view.amount(), view.createdAt(), view.read());
    }

    /** Same Spring Page JSON as the transaction history, so the frontend reads both the same way. */
    public Page<NotificationResponseBody> toPage(PageResult<NotificationView> result) {
        PageRequest pageRequest = PageRequest.of(result.page(), result.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return new PageImpl<>(result.content().stream().map(this::toResponse).toList(), pageRequest,
                result.totalElements());
    }
}
