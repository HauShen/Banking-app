package com.Banking_app.security;
import com.Banking_app.userProfile.adapter.out.security.SecurityUserDetails;
import com.Banking_app.userProfile.application.port.out.LoadUserProfilePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService{
    private final LoadUserProfilePort loadUserProfilePort;
    @Autowired
    public CustomUserDetailsService(LoadUserProfilePort loadUserProfilePort){
        this.loadUserProfilePort = loadUserProfilePort;
    }
    @Override
    public SecurityUserDetails loadUserByUsername(String username) {   // narrower return type: callers need getUserId()
        return loadUserProfilePort.findByUsername(username)
                .map(SecurityUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
