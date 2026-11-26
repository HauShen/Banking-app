package com.Banking_app.demo.adapter.in.web;

import com.Banking_app.demo.DemoCast;
import com.Banking_app.demo.DemoProperties;
import com.Banking_app.demo.DemoUser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The demo logins for the login page (plan step 8). Public (SecurityConfig), because the visitor isn't logged in yet.
 *
 * <p>Not limited to the demo profile on purpose, unlike the rest of the demo package: the login page asks on every
 * visit, and outside the demo an unknown URL would end in the catch-all exception handler as a 500 with an error
 * log. So the endpoint always exists and answers <b>404 without a body</b> when the demo profile is off (no
 * {@link DemoProperties} bean). The page then simply shows no demo box.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoLoginsController {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private final ObjectProvider<DemoProperties> properties;
    private final Clock clock;
    private final ZoneId zone;
    private final String resetCron;

    public DemoLoginsController(ObjectProvider<DemoProperties> properties, Clock clock,
                                @Value("${bank.time-zone:Asia/Kuala_Lumpur}") ZoneId zone,
                                @Value("${app.demo.reset-cron:" + Scheduled.CRON_DISABLED + "}") String resetCron) {
        this.properties = properties;
        this.clock = clock;
        this.zone = zone;
        this.resetCron = resetCron;
    }

    @GetMapping("/logins")
    public ResponseEntity<DemoLoginsResponse> logins() {
        DemoProperties demo = properties.getIfAvailable();
        if (demo == null) {
            return ResponseEntity.notFound().build();
        }
        DemoCast cast = DemoCast.standard(demo.historyDays());
        List<String> otherCustomers = cast.users().stream()
                .filter(user -> !user.isAdmin() && !user.username().equals(DemoCast.TOUR_CUSTOMER))
                .map(DemoUser::username)
                .toList();
        String admin = cast.users().stream().filter(DemoUser::isAdmin).findFirst().orElseThrow().username();

        return ResponseEntity.ok(new DemoLoginsResponse(
                new DemoLoginsResponse.Login(DemoCast.TOUR_CUSTOMER, demo.customerPassword()),
                new DemoLoginsResponse.Login(admin, demo.adminPassword()),
                otherCustomers,
                nextResetTime(),
                zone.getId()));
    }

    /** "04:00" for "0 0 4 * * *": the local time of the next reset in the bank's time zone; null when it's off. */
    private String nextResetTime() {
        if (Scheduled.CRON_DISABLED.equals(resetCron)) {
            return null;
        }
        ZonedDateTime next = CronExpression.parse(resetCron).next(ZonedDateTime.now(clock).withZoneSameInstant(zone));
        return next == null ? null : next.format(HOUR_MINUTE);
    }
}
