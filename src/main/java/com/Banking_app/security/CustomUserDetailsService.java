package com.Banking_app.security;
import com.Banking_app.userProfile.adapter.out.persistence.jparepositories.UserProfileJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements  UserDetailsService{
    private final UserProfileJpaRepository userProfileJpaRepository;
    @Autowired
    public CustomUserDetailsService(UserProfileJpaRepository userProfileJpaRepository){
        this.userProfileJpaRepository = userProfileJpaRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        return userProfileJpaRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
