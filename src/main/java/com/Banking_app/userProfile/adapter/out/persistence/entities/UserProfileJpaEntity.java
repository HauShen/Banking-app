package com.Banking_app.userProfile.adapter.out.persistence.entities;
import com.Banking_app.jpaentities.Account;
import com.Banking_app.userProfile.domain.enums.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Table;


import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class UserProfileJpaEntity{
    @Id
    @Column(name = "id", nullable = false)
    private String id;
    @Column(name = "username", nullable = false)
    private String username;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(name = "email", nullable = false)
    private String email;
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "created_at",nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at",nullable = false)
    private Instant updatedAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", orphanRemoval = true )
    private List<Account> accounts;




    public UserProfileJpaEntity(String id, String username, UserRole role, String fullName, String email, String password, Instant createdAt, Instant updatedAt){
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public UserProfileJpaEntity(){}
}
