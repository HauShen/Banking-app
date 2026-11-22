package com.Banking_app.notification.adapter.in.web.controllers;

import com.Banking_app.notification.adapter.in.web.dto.MarkAllReadResponseBody;
import com.Banking_app.notification.adapter.in.web.dto.NotificationResponseBody;
import com.Banking_app.notification.adapter.in.web.dto.UnreadCountResponseBody;
import com.Banking_app.notification.adapter.in.web.mapper.NotificationWebMapper;
import com.Banking_app.notification.application.port.in.query.ListNotificationsQuery;
import com.Banking_app.notification.application.port.in.usecase.ListNotificationsUseCase;
import com.Banking_app.notification.application.port.in.usecase.MarkNotificationsReadUseCase;
import com.Banking_app.notification.security.CurrentRequester;
import com.Banking_app.notification.shared.Requester;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in user's own notifications (customers and admins alike; nobody sees anyone else's). Any logged-in user
 * may call these; without a token → 401 (SecurityConfig: /api/notifications/** needs a valid token).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final MarkNotificationsReadUseCase markNotificationsReadUseCase;
    private final NotificationWebMapper mapper;

    public NotificationController(ListNotificationsUseCase listNotificationsUseCase,
                                  MarkNotificationsReadUseCase markNotificationsReadUseCase,
                                  NotificationWebMapper mapper) {
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.markNotificationsReadUseCase = markNotificationsReadUseCase;
        this.mapper = mapper;
    }

    /** Newest first. {@code size} 1..100 (default 20), {@code page} from 0; bad values → 400. */
    @GetMapping
    public ResponseEntity<Page<NotificationResponseBody>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentRequester Requester requester) {
        var result = listNotificationsUseCase.list(
                new ListNotificationsQuery(requester.userId(), unreadOnly, page, size));
        return ResponseEntity.ok(mapper.toPage(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseBody> unreadCount(@CurrentRequester Requester requester) {
        return ResponseEntity.ok(new UnreadCountResponseBody(listNotificationsUseCase.countUnread(requester.userId())));
    }

    /** 204; also for one that was already read. Unknown or someone else's → 404. */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable long id, @CurrentRequester Requester requester) {
        markNotificationsReadUseCase.markRead(requester.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<MarkAllReadResponseBody> markAllRead(@CurrentRequester Requester requester) {
        return ResponseEntity.ok(new MarkAllReadResponseBody(markNotificationsReadUseCase.markAllRead(
                requester.userId())));
    }
}
