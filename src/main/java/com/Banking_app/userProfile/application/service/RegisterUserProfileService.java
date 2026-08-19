package com.Banking_app.userProfile.application.service;
import com.Banking_app.exception.ResourceNotFoundException;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.in.command.RegisterUserProfileCommand;
import com.Banking_app.userProfile.application.port.in.usecase.RegisterUserProfileUseCase;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.PasswordHasherPort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;

@Service
public class RegisterUserProfileService implements RegisterUserProfileUseCase
{
    private final LoadUserProfilePort loadUserProfilePort;
    private final SaveUserProfilePort saveUserProfilePort;
    private final PasswordHasherPort passwordHasher;
    private final Clock clock;

    public RegisterUserProfileService(LoadUserProfilePort loadUserProfilePort,
                                      SaveUserProfilePort saveUserProfilePort,
                                      PasswordHasherPort passwordHasher,
                                      Clock clock) {
        this.loadUserProfilePort = loadUserProfilePort;
        this.saveUserProfilePort = saveUserProfilePort;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }
    @Override
    @Transactional
    public UserProfile register(RegisterUserProfileCommand command){
        if (loadUserProfilePort.existsByUsername(command.username())) {
            throw new DuplicateUserException("Username already exists");
        }
        if (loadUserProfilePort.existsByEmail(command.email())) {
            throw new DuplicateUserException("Email already exists");
        }
        String passwordHash = passwordHasher.hash(command.rawPassword());

        UserProfile newUser = UserProfile.register(
                UUID.randomUUID().toString(),
                command.username(),
                command.fullName(),
                command.email(),
                passwordHash,
                clock.instant());

        return saveUserProfilePort.save(newUser);
    }
}
