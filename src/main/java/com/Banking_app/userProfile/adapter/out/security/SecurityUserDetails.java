package com.Banking_app.userProfile.adapter.out.security;

import com.Banking_app.userProfile.domain.models.userprofile.UserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class SecurityUserDetails implements UserDetails {
    private final UserProfile userProfile;

    public SecurityUserDetails(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    public String getUserId() {
        return userProfile.getId();
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userProfile.getRole().name()));
    }

    @Override
    public String getPassword() {
        return userProfile.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return userProfile.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
