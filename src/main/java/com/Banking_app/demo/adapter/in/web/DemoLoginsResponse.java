package com.Banking_app.demo.adapter.in.web;

import java.util.List;

/**
 * What the login page shows in its "Try the demo" box (GET /api/demo/logins). The passwords are public on purpose:
 * they only exist with the demo profile.
 *
 * @param customer       the customer the tour starts with (weiming)
 * @param admin          the demo admin (demoadmin)
 * @param otherCustomers the other demo customers, same password as {@code customer}
 * @param resetTime      local time of the nightly reset, e.g. "04:00"; null if the reset is switched off
 * @param timeZone       the bank's time zone that {@code resetTime} is in, e.g. "Asia/Kuala_Lumpur"
 */
public record DemoLoginsResponse(Login customer, Login admin, List<String> otherCustomers, String resetTime,
                                 String timeZone) {

    public record Login(String username, String password) {
    }
}
