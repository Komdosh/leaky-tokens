package com.leaky.tokens.tokenservice.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.tokenservice.flags.TokenServiceFeatureFlags;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TokenQuotaServiceTest {
    @Test
    void reserveReturnsAllowedWhenFeatureDisabled() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        TokenQuotaProperties properties = new TokenQuotaProperties();
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(false);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, properties, flags);
        TokenQuotaReservation reservation = service.reserve(UUID.randomUUID(), "openai", 50, null);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void reserveReturnsPoolValuesWhenFeatureDisabledAndPoolExists() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        TokenQuotaProperties properties = new TokenQuotaProperties();
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(false);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            100,
            75,
            Instant.now().plus(Duration.ofHours(1)),
            Instant.now(),
            Instant.now()
        );
        when(repository.findForUpdate(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, properties, flags);
        TokenQuotaReservation reservation = service.reserve(userId, "openai", 50, null);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.total()).isEqualTo(100);
        assertThat(reservation.remaining()).isEqualTo(75);
    }

    @Test
    void reserveRejectsWhenPoolMissing() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        when(repository.findForUpdate(any(), any())).thenReturn(Optional.empty());

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenQuotaReservation reservation = service.reserve(UUID.randomUUID(), "openai", 50, null);

        assertThat(reservation.allowed()).isFalse();
        assertThat(reservation.total()).isEqualTo(0);
        assertThat(reservation.remaining()).isEqualTo(0);
    }

    @Test
    void reserveAppliesQuotaCapAndPersists() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            500,
            500,
            Instant.now().plus(Duration.ofHours(1)),
            Instant.now(),
            Instant.now()
        );
        when(repository.findForUpdate(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setQuotaMaxTokens(100L);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenQuotaReservation reservation = service.reserve(userId, "openai", 50, tier);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(50);

        ArgumentCaptor<TokenPool> captor = ArgumentCaptor.forClass(TokenPool.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRemainingTokens()).isEqualTo(50);
    }

    @Test
    void reserveResetsExpiredWindow() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        Instant past = Instant.now().minus(Duration.ofHours(2));
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            1000,
            100,
            past,
            Instant.now(),
            Instant.now()
        );
        when(repository.findForUpdate(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenQuotaReservation reservation = service.reserve(userId, "openai", 100, null);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(900);
    }

    @Test
    void addTokensCreatesPoolWithResetTime() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        when(repository.findForUpdate(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenPool pool = service.addTokens(UUID.randomUUID(), "openai", 200, null);

        assertThat(pool.getTotalTokens()).isEqualTo(200);
        assertThat(pool.getRemainingTokens()).isEqualTo(200);
        assertThat(pool.getResetTime()).isNotNull();
        assertThat(Duration.between(pool.getCreatedAt(), pool.getResetTime()).toHours()).isEqualTo(24);
    }

    @Test
    void reserveOrgHonorsFeatureFlags() {
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(false);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), flags);
        TokenQuotaReservation reservation = service.reserveOrg(UUID.randomUUID(), "openai", 10, null);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void reserveOrgAppliesQuotaCapAndPersists() {
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);

        UUID orgId = UUID.randomUUID();
        OrgTokenPool pool = new OrgTokenPool(
            UUID.randomUUID(),
            orgId,
            "openai",
            500,
            500,
            Instant.now().plus(Duration.ofHours(1)),
            Instant.now(),
            Instant.now()
        );
        when(orgRepository.findForUpdate(eq(orgId), eq("openai"))).thenReturn(Optional.of(pool));
        when(orgRepository.save(any(OrgTokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setQuotaMaxTokens(200L);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenQuotaReservation reservation = service.reserveOrg(orgId, "openai", 50, tier);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(150);
    }

    @Test
    void addTokensSkipsResetWhenQuotaDisabled() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        when(repository.findForUpdate(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(false);
        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), flags);

        TokenPool pool = service.addTokens(UUID.randomUUID(), "openai", 100, null);

        assertThat(pool.getResetTime()).isNull();
    }

    @Test
    void reserveReturnsAllowedWhenTierCapNotPositive() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            100,
            100,
            Instant.now().plus(Duration.ofHours(1)),
            Instant.now(),
            Instant.now()
        );
        when(repository.findForUpdate(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setQuotaMaxTokens(0L);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenQuotaReservation reservation = service.reserve(userId, "openai", 20, tier);

        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(80);
    }

    @Test
    void reserveRejectsWhenQuotaCapBelowRequestedTokens() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            100,
            100,
            Instant.now().plus(Duration.ofHours(1)),
            Instant.now(),
            Instant.now()
        );
        when(repository.findForUpdate(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setQuotaMaxTokens(20L);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        TokenQuotaReservation reservation = service.reserve(userId, "openai", 30, tier);

        assertThat(reservation.allowed()).isFalse();
        assertThat(reservation.remaining()).isEqualTo(20);
        verify(repository, never()).save(any(TokenPool.class));
    }

    @Test
    void releaseCapsRemainingToTierLimit() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            200,
            10,
            Instant.now().plus(Duration.ofHours(1)),
            Instant.now(),
            Instant.now()
        );
        when(repository.findForUpdate(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setQuotaMaxTokens(50L);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        service.release(userId, "openai", 100, tier);

        assertThat(pool.getRemainingTokens()).isEqualTo(50);
    }

    @Test
    void getQuotaEnsuresResetTimeWhenMissing() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            100,
            90,
            null,
            Instant.now(),
            Instant.now()
        );
        when(repository.findByUserIdAndProvider(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, quotaProps(), featureFlags());
        service.getQuota(userId, "openai", null);

        verify(repository).save(any(TokenPool.class));
        assertThat(pool.getResetTime()).isNotNull();
    }

    @Test
    void getQuotaSkipsResetWhenWindowInvalid() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);

        UUID userId = UUID.randomUUID();
        TokenPool pool = new TokenPool(
            UUID.randomUUID(),
            userId,
            "openai",
            100,
            90,
            null,
            Instant.now(),
            Instant.now()
        );
        when(repository.findByUserIdAndProvider(eq(userId), eq("openai"))).thenReturn(Optional.of(pool));

        TokenQuotaProperties properties = new TokenQuotaProperties();
        properties.setEnabled(true);
        properties.setWindow(null);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, properties, featureFlags());
        service.getQuota(userId, "openai", null);

        verify(repository, never()).save(any(TokenPool.class));
        assertThat(pool.getResetTime()).isNull();
    }

    @Test
    void addTokensDoesNotSetResetWhenWindowInvalid() {
        TokenPoolRepository repository = Mockito.mock(TokenPoolRepository.class);
        OrgTokenPoolRepository orgRepository = Mockito.mock(OrgTokenPoolRepository.class);
        when(repository.findForUpdate(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any(TokenPool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenQuotaProperties properties = new TokenQuotaProperties();
        properties.setEnabled(true);
        properties.setWindow(null);

        TokenQuotaService service = new TokenQuotaService(repository, orgRepository, properties, featureFlags());
        TokenPool pool = service.addTokens(UUID.randomUUID(), "openai", 100, null);

        assertThat(pool.getResetTime()).isNull();
    }

    private TokenQuotaProperties quotaProps() {
        TokenQuotaProperties properties = new TokenQuotaProperties();
        properties.setEnabled(true);
        properties.setWindow(Duration.ofHours(24));
        return properties;
    }

    private TokenServiceFeatureFlags featureFlags() {
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(true);
        return flags;
    }
}
