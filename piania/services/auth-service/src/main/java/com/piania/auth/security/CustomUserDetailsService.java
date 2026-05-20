package com.piania.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.piania.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .map(user -> User.builder()
                        .username(user.getEmail())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
