package com.leaky.tokens.authserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.Role;
import com.leaky.tokens.authserver.domain.UserAccount;
import com.leaky.tokens.authserver.dto.AuthResponse;
import com.leaky.tokens.authserver.dto.LoginRequest;
import com.leaky.tokens.authserver.dto.RegisterRequest;
import com.leaky.tokens.authserver.repo.RoleRepository;
import com.leaky.tokens.authserver.repo.UserAccountRepository;
import com.leaky.tokens.authserver.service.AuthService;
import com.leaky.tokens.authserver.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    @Test
    void registerAssignsDefaultRole() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(eq("alice@example.com"))).thenReturn(Optional.empty());
        when(passwordEncoder.encode(eq("secret"))).thenReturn("hashed");
        when(jwtService.issueToken(any(UserAccount.class))).thenReturn("token-123");

        Role userRole = new Role(UUID.randomUUID(), "USER", "Default user role");
        when(roleRepository.findByName(eq("USER"))).thenReturn(Optional.of(userRole));

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        AuthResponse response = authService.register(request);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.roles()).containsExactly("USER");

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).contains(userRole);
    }

    @Test
    void loginAssignsDefaultRoleWhenMissing() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        UserAccount user = new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hashed");
        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("secret"), eq("hashed"))).thenReturn(true);
        when(jwtService.issueToken(any(UserAccount.class))).thenReturn("token-456");

        Role userRole = new Role(UUID.randomUUID(), "USER", "Default user role");
        when(roleRepository.findByName(eq("USER"))).thenReturn(Optional.of(userRole));

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        AuthResponse response = authService.login(request);

        assertThat(response.roles()).containsExactly("USER");
        verify(userRepository).save(any(UserAccount.class));
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        UserAccount user = new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hashed");
        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("wrong"), eq("hashed"))).thenReturn(false);

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid credentials");
    }

    @Test
    void loginRejectsUnknownUser() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.empty());

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid credentials");
    }

    @Test
    void loginRejectsMissingUsernameOrPassword() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        LoginRequest request = new LoginRequest();
        request.setUsername(" ");
        request.setPassword(" ");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("username and password are required");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.of(
            new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hashed")
        ));

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("username already exists");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(eq("alice@example.com"))).thenReturn(Optional.of(
            new UserAccount(UUID.randomUUID(), "bob", "alice@example.com", "hashed")
        ));

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("email already exists");
    }

    @Test
    void registerCreatesDefaultRoleWhenMissing() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        when(userRepository.findByUsername(eq("alice"))).thenReturn(Optional.empty());
        when(userRepository.findByEmail(eq("alice@example.com"))).thenReturn(Optional.empty());
        when(passwordEncoder.encode(eq("secret"))).thenReturn("hashed");
        when(jwtService.issueToken(any(UserAccount.class))).thenReturn("token-123");
        when(roleRepository.findByName(eq("USER"))).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        AuthResponse response = authService.register(request);

        assertThat(response.roles()).containsExactly("USER");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void registerRejectsMissingUsernameOrEmail() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername(null);
        request.setEmail(null);
        request.setPassword("secret");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("username, email, and password are required");
    }

    @Test
    void registerRejectsMissingFields() {
        UserAccountRepository userRepository = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtService jwtService = Mockito.mock(JwtService.class);

        AuthService authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword(" ");

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("username, email, and password are required");
    }
}
