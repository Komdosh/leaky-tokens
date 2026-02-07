package com.leaky.tokens.authserver.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

class AuthorizationServerConfigTest {
    @Test
    void registeredClientRepositoryContainsExpectedClient() {
        AuthorizationServerConfig config = new AuthorizationServerConfig();
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        RegisteredClientRepository repository = config.registeredClientRepository(encoder);
        RegisteredClient client = repository.findByClientId("leaky-client");

        assertThat(client).isNotNull();
        assertThat(client.getClientAuthenticationMethods())
            .contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        assertThat(client.getAuthorizationGrantTypes())
            .contains(AuthorizationGrantType.CLIENT_CREDENTIALS);
        assertThat(client.getScopes())
            .contains("tokens.read", "tokens.write");
        assertThat(encoder.matches("leaky-secret", client.getClientSecret())).isTrue();
    }

    @Test
    void authorizationServerSettingsBuildsDefaults() {
        AuthorizationServerConfig config = new AuthorizationServerConfig();

        AuthorizationServerSettings settings = config.authorizationServerSettings();

        assertThat(settings).isNotNull();
        assertThat(settings.getIssuer()).isNull();
    }
}
