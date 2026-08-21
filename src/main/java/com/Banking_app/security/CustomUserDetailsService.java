package com.Banking_app.security;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService{
    private final UserProfileJpaRepository userProfileJpaRepository;
    private final LoadUserProfilePort loadUserProfilePort;
    @Autowired
    public CustomUserDetailsService(UserProfileJpaRepository userProfileJpaRepository, LoadUserProfilePort loadUserProfilePort){
        this.userProfileJpaRepository = userProfileJpaRepository;
        this.loadUserProfilePort = loadUserProfilePort;
    }
    @Override
    public UserDetails loadUserByUsername(String username) {
        return loadUserProfilePort.findByUsername(username)
                .map(SecurityUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
