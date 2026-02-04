package com.leaky.tokens.authserver.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    private final RSAKey rsaKey = generateRsa();

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public RSAKey rsaJwk() {
        return rsaKey;
    }

    public String signToken(String subject, String issuer, Instant expiresAt) {
        return signToken(subject, issuer, expiresAt, java.util.List.of());
    }

    public String signToken(String subject, String issuer, Instant expiresAt, java.util.List<String> roles) {
        try {
            JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .claim("roles", roles == null ? java.util.List.of() : roles)
                .expirationTime(java.util.Date.from(expiresAt))
                .issueTime(java.util.Date.from(Instant.now()))
                .jwtID(UUID.randomUUID().toString())
                .build();

            SignedJWT signedJWT = new SignedJWT(
                new com.nimbusds.jose.JWSHeader(JWSAlgorithm.RS256),
                claims
            );
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private RSAKey generateRsa() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA keys", e);
        }
    }
}
