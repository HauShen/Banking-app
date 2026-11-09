package com.Banking_app.fx.domain.enums;

/**
 * Which way the money goes, seen from the customer. It decides the rate, as banks quote it from their own side.
 */
public enum ConversionDirection {

    /** The customer pays in a foreign currency (e.g. a transfer in USD): the bank sells it to them → sell rate. */
    PAY,

    /** The customer receives foreign money (e.g. a remittance from abroad): the bank buys it → buy rate. */
    RECEIVE
}
