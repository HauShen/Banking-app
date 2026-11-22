package com.Banking_app.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * notification-service: the in-app notifications, as a service of its own (it was a module of the main app).
 *
 * <ul>
 *   <li>Reads the transaction events from Kafka (consumer group "notification-service") and stores notifications in
 *       its own database, notifications_db.</li>
 *   <li>Serves /api/notifications. It checks the main app's access tokens itself (same JWT_SECRET) and never calls
 *       the main app.</li>
 * </ul>
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
