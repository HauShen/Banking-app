package com.Banking_app.dto.requestBodies;

import com.Banking_app.models.enums.AccountStatus;
import com.Banking_app.models.enums.AccountType;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Data
public class AccountRequestBody {
    @NotNull(message = "userId is required")
    private String userId;

    @NotNull(message = "accountType is required")
    private AccountType accountType; //SAVINGS or CURRENT

    public AccountRequestBody(){}

}
