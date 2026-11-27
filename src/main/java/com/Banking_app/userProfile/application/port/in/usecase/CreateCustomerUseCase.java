package com.Banking_app.userProfile.application.port.in.usecase;

import com.Banking_app.userProfile.application.port.in.command.CreateCustomerCommand;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;

/** An admin creates a customer profile. The only way customers are created (no public sign-up). */
public interface CreateCustomerUseCase {
    UserProfile createCustomer(CreateCustomerCommand command);
}
