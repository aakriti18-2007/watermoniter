package com.example.watermoniter.service;

import com.example.watermoniter.model.AppUser;
import com.example.watermoniter.repository.AppUserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seedUsers() {
        createIfMissing("admin", "admin123", "ADMIN", "System Admin");
        createIfMissing("operator", "operator123", "OPERATOR", "Plant Operator");
        createIfMissing("viewer", "viewer123", "VIEWER", "Control Viewer");
    }

    public List<AppUser> listUsers() {
        return appUserRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getUsername))
                .toList();
    }

    public AppUser findDomainUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public AppUser createUser(String username, String rawPassword, String role, String displayName) {
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        validateRole(role);

        return appUserRepository.save(new AppUser(
                username,
                passwordEncoder.encode(rawPassword),
                role,
                displayName
        ));
    }

    public AppUser updateUser(Long id, String displayName, String role) {
        validateRole(role);
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setDisplayName(displayName);
        user.setRole(role);
        return appUserRepository.save(user);
    }

    public AppUser resetPassword(Long id, String rawPassword) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(rawPassword));
        return appUserRepository.save(user);
    }

    public void deleteUser(Long id, String currentUsername) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("You cannot delete the current logged-in admin");
        }

        appUserRepository.delete(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = findDomainUser(username);
        return new User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    private void createIfMissing(String username, String rawPassword, String role, String displayName) {
        if (appUserRepository.findByUsername(username).isPresent()) {
            return;
        }

        createUser(username, rawPassword, role, displayName);
    }

    private void validateRole(String role) {
        if (!List.of("ADMIN", "OPERATOR", "VIEWER").contains(role)) {
            throw new IllegalArgumentException("Invalid role");
        }
    }
}
