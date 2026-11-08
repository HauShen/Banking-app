package com.Banking_app.account.application.port.in.result;

import com.Banking_app.account.domain.models.account.Account;

import java.math.BigDecimal;
import java.util.List;

public record AccountSummary(BigDecimal totalBalance, List<Account> accounts){
}
