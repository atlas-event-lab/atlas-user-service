package com.atlas.user.profile.service;

import com.atlas.user.profile.dto.BootstrapProfileRequest;
import com.atlas.user.profile.dto.UserProfileResponse;
import com.atlas.user.profile.entity.ProfileStatus;
import com.atlas.user.profile.entity.UserProfile;
import com.atlas.user.profile.mapper.UserProfileMapper;
import com.atlas.user.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final String SUB = "11111111-1111-1111-1111-111111111111";
    private static final String EMAIL = "user@atlas.local";

    @Mock UserProfileRepository repository;
    @Mock UserProfileMapper mapper;

    @InjectMocks
    ProfileServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private Jwt fullClaimsJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", SUB)
                .claim("email", EMAIL)
                .claim("given_name", "Ada")
                .claim("family_name", "Lovelace")
                .build();
    }

    private UserProfileResponse anyResponse() {
        return new UserProfileResponse(UUID.randomUUID(), EMAIL, "Ada", "Lovelace",
                null, null, ProfileStatus.ACTIVE, null, null);
    }

    // ── AC-1: create on first call ────────────────────────────────────────────

    @Test
    void createsActiveProfileFromClaimsOnFirstCall() {
        authenticate(fullClaimsJwt());
        when(repository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(anyResponse());

        BootstrapResult result = service.bootstrap(null);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).saveAndFlush(captor.capture());
        UserProfile persisted = captor.getValue();
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getKeycloakUserId()).isEqualTo(SUB);
        assertThat(persisted.getEmail()).isEqualTo(EMAIL);
        assertThat(persisted.getFirstName()).isEqualTo("Ada");
        assertThat(persisted.getLastName()).isEqualTo("Lovelace");
        assertThat(persisted.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    }

    // ── AC-2: idempotent on repeat ────────────────────────────────────────────

    @Test
    void returnsExistingProfileWithoutWritingOnRepeat() {
        authenticate(fullClaimsJwt());
        UserProfile existing = new UserProfile();
        existing.setId(UUID.randomUUID());
        existing.setKeycloakUserId(SUB);
        when(repository.findByKeycloakUserId(SUB)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(anyResponse());

        BootstrapResult result = service.bootstrap(null);

        assertThat(result.created()).isFalse();
        verify(repository, never()).saveAndFlush(any());
    }

    // ── AC-3: identity from token, body ignored for identity ──────────────────

    @Test
    void takesIdentityFromTokenAndSeedFromBodyOnCreate() {
        authenticate(fullClaimsJwt());
        when(repository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(anyResponse());

        service.bootstrap(new BootstrapProfileRequest("Ada L.", "+34911234567"));

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).saveAndFlush(captor.capture());
        UserProfile persisted = captor.getValue();
        assertThat(persisted.getKeycloakUserId()).isEqualTo(SUB);
        assertThat(persisted.getEmail()).isEqualTo(EMAIL);
        assertThat(persisted.getDisplayName()).isEqualTo("Ada L.");
        assertThat(persisted.getPhoneNumber()).isEqualTo("+34911234567");
    }

    // ── AC-5: concurrent first calls (UNIQUE race) ────────────────────────────

    @Test
    void recoversFromUniqueRaceAndReturnsWinningRow() {
        authenticate(fullClaimsJwt());
        UserProfile winner = new UserProfile();
        winner.setId(UUID.randomUUID());
        winner.setKeycloakUserId(SUB);
        when(repository.findByKeycloakUserId(SUB))
                .thenReturn(Optional.empty())   // first read: not yet provisioned
                .thenReturn(Optional.of(winner)); // re-read after the race
        when(repository.saveAndFlush(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException("uq_user_profiles_keycloak_user_id"));
        when(mapper.toResponse(winner)).thenReturn(anyResponse());

        BootstrapResult result = service.bootstrap(null);

        assertThat(result.created()).isFalse();
        verify(repository, never()).save(any());
    }

    // ── AC-6: create-only seed (existing profile not mutated) ─────────────────

    @Test
    void doesNotApplyBodySeedToExistingProfile() {
        authenticate(fullClaimsJwt());
        UserProfile existing = new UserProfile();
        existing.setId(UUID.randomUUID());
        existing.setKeycloakUserId(SUB);
        existing.setDisplayName(null);
        when(repository.findByKeycloakUserId(SUB)).thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(anyResponse());

        service.bootstrap(new BootstrapProfileRequest("New Name", "+34911234567"));

        assertThat(existing.getDisplayName()).isNull();
        assertThat(existing.getPhoneNumber()).isNull();
        verify(repository, never()).saveAndFlush(any());
    }

    // ── Optional claims absent → stored as null, profile still created ────────

    @Test
    void storesNullNamesWhenOptionalClaimsAbsent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", SUB)
                .claim("email", EMAIL)
                .build();
        authenticate(jwt);
        when(repository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(UserProfile.class))).thenReturn(anyResponse());

        BootstrapResult result = service.bootstrap(null);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFirstName()).isNull();
        assertThat(captor.getValue().getLastName()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    }
}
