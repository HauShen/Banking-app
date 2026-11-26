package com.Banking_app.demo;

/** How many rows {@link DemoDataWriter} inserted, for the seeder's log line. */
public record DemoWriteResult(int users, int accounts, int transactions, int ledgerRows) {
}
