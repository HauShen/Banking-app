package com.Banking_app.service;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class MyUserDetailsService implements  UserDetailsService{
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        // In a real app, you would load the user from a database
        // For this example, we'll use a hardcoded user
        if ("user".equals(username)) {
            return new User("user", "$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6", new ArrayList<>()); // password is "password"
        }
        else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}
