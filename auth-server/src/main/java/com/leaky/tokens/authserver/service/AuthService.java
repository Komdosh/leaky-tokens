package com.leaky.tokens.authserver.service;

import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.Role;
import com.leaky.tokens.authserver.domain.UserAccount;
import com.leaky.tokens.authserver.dto.AuthResponse;
import com.leaky.tokens.authserver.dto.LoginRequest;
import com.leaky.tokens.authserver.dto.RegisterRequest;
import com.leaky.tokens.authserver.repo.RoleRepository;
import com.leaky.tokens.authserver.repo.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());
        String password = request.getPassword();

        if (username == null || email == null || password == null || password.isBlank()) {
            throw new IllegalArgumentException("username, email, and password are required");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("email already exists");
        }

        UserAccount user = new UserAccount(UUID.randomUUID(), username, email, passwordEncoder.encode(password));
        assignDefaultRole(user);
        userRepository.save(user);

        return new AuthResponse(user.getId(), user.getUsername(), jwtService.issueToken(user), rolesFor(user));
    }

    public AuthResponse login(LoginRequest request) {
        String username = normalize(request.getUsername());
        String password = request.getPassword();
        if (username == null || password == null || password.isBlank()) {
            throw new IllegalArgumentException("username and password are required");
        }

        Optional<UserAccount> user = userRepository.findByUsername(username);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        UserAccount account = user.get();
        if (account.getRoles().isEmpty()) {
            assignDefaultRole(account);
            userRepository.save(account);
        }
        return new AuthResponse(account.getId(), account.getUsername(), jwtService.issueToken(account), rolesFor(account));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private void assignDefaultRole(UserAccount user) {
        Role userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(new Role(UUID.randomUUID(), "USER", "Default user role")));
        user.addRole(userRole);
    }

    private java.util.List<String> rolesFor(UserAccount user) {
        return user.getRoles().stream()
            .map(Role::getName)
            .sorted()
            .toList();
    }
}
