package com.Banking_app.userProfile.adapter.out.persistence;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import com.Banking_app.userProfile.adapter.out.persistence.mappers.UserProfilePersistenceMapper;
import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserProfilePersistenceAdapter implements LoadUserProfilePort,SaveUserProfilePort {
    private final UserProfileJpaRepository repository;
    private final UserProfilePersistenceMapper mapper;

    public UserProfilePersistenceAdapter(UserProfileJpaRepository repository,
                                         UserProfilePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    @Override
    public Optional<UserProfile> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        try {
            UserProfileJpaEntity entity = repository.findById(userProfile.getId())
                    .map(managed -> {
                        mapper.updateEntity(managed, userProfile);
                        return managed;
                    })
                    .orElseGet(() -> mapper.toNewEntity(userProfile));

            return mapper.toDomain(repository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            // The existsBy... check in the service is a courtesy; the unique index is the
            // actual guarantee. Two simultaneous registrations both pass the check and one
            // of them lands here — translate it into the same failure the core understands.
            throw new DuplicateUserException("Username or email already exists", ex);
        }
    }
    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsAnyAdmin() {
        return repository.existsByRole(UserRole.ADMIN);
    }

    @Override
    public long countAdmins() {
        return repository.countByRole(UserRole.ADMIN);
    }
}
