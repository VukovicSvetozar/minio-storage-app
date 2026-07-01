package com.minio.storage.service;

import com.minio.storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        String normalized = usernameOrEmail.toLowerCase().trim();
        return userRepository.findByUsername(normalized)
                .or(() -> userRepository.findByEmail(normalized))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Korisnik nije pronađen: " + usernameOrEmail
                ));
    }

}