package com.Banking_app.userProfile.adapter.out.persistence;
import com.Banking_app.userProfile.adapter.out.persistence.entities.UserProfileJpaEntity;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import com.Banking_app.userProfile.adapter.out.persistence.mappers.UserProfilePersistenceMapper;
import com.Banking_app.userProfile.application.exception.DuplicateUserException;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import com.Banking_app.userProfile.application.port.out.SaveUserProfilePort;
import com.Banking_app.userProfile.common.PageResult;
import com.Banking_app.userProfile.domain.enums.UserRole;
import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import com.Banking_app.userProfile.application.port.in.query.ListUserProfilesQuery;
import com.Banking_app.userProfile.application.port.in.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserProfilePersistenceAdapter implements LoadUserProfilePort,SaveUserProfilePort {
    private final UserProfileJpaRepository repository;
    private final UserProfilePersistenceMapper mapper;

    /**
     * Translates the core's sort vocabulary into Spring Data's.
     *
     * <p>This mapping is why the port takes an enum: entity property names are a persistence
     * detail, and they belong in the only class allowed to know them.
     */
    private static Sort toSort(ListUserProfilesQuery query) {
        String property = switch (query.sortBy()) {
            case USERNAME -> "username";
            case FULL_NAME -> "fullName";
            case EMAIL -> "email";
            case ROLE -> "role";
            case CREATED_AT -> "createdAt";
        };

        // Secondary sort on the primary key. Without a tiebreaker, rows sharing a sort value can
        // appear on two pages or none — Postgres promises no ordering beyond the ORDER BY clause,
        // and LIMIT/OFFSET re-runs the query per page. Sorting by ROLE with two roles and 40 users
        // makes this reproducible in about a minute.
        Sort.Direction direction = query.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

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
    @Override
    public PageResult<UserProfile> findAll(ListUserProfilesQuery query) {
        Page<UserProfileJpaEntity> page = repository.findAll(
                PageRequest.of(query.page(), query.size(), toSort(query)));

        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }
    @Override
    public Optional<UserProfile> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

}
