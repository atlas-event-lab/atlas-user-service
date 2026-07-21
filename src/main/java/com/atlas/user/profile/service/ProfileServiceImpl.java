package com.atlas.user.profile.service;

import com.atlas.user.profile.dto.BootstrapProfileRequest;
import com.atlas.user.profile.entity.ProfileStatus;
import com.atlas.user.profile.entity.UserProfile;
import com.atlas.user.profile.mapper.UserProfileMapper;
import com.atlas.user.profile.repository.UserProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    /**
     * Orchestration is intentionally non-transactional: the single write is atomic
     * ({@code saveAndFlush} = one local transaction) and, on a UNIQUE(keycloak_user_id)
     * race, that transaction must fully roll back before we re-read the winning row in a
     * fresh one. Wrapping the whole method in a transaction would poison that re-read.
     */
    @Override
    public BootstrapResult bootstrap(BootstrapProfileRequest request) {
        Jwt jwt = currentJwt();
        String keycloakUserId = jwt.getSubject();

        // Idempotent: an existing profile is returned unchanged (200). The optional
        // displayName/phoneNumber are NOT applied to an existing profile (create-only seed).
        var existing = repository.findByKeycloakUserId(keycloakUserId);
        if (existing.isPresent()) {
            return new BootstrapResult(mapper.toResponse(existing.get()), false);
        }

        try {
            UserProfile created = create(jwt, keycloakUserId, request);
            log.info("Provisioned UserProfile id={}", created.getId());
            return new BootstrapResult(mapper.toResponse(created), true);
        } catch (DataIntegrityViolationException race) {
            // Concurrent first call won the UNIQUE(keycloak_user_id) race: re-read and
            // return the winning row (200, no duplicate, no 500) — feature Idempotency & Concurrency.
            UserProfile winner = repository.findByKeycloakUserId(keycloakUserId).orElseThrow(() -> race);
            return new BootstrapResult(mapper.toResponse(winner), false);
        }
    }

    private UserProfile create(Jwt jwt, String keycloakUserId, BootstrapProfileRequest request) {
        UserProfile profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setKeycloakUserId(keycloakUserId);
        profile.setEmail(jwt.getClaimAsString("email"));
        profile.setFirstName(jwt.getClaimAsString("given_name"));
        profile.setLastName(jwt.getClaimAsString("family_name"));
        profile.setStatus(ProfileStatus.ACTIVE);

        if (request != null) {
            profile.setDisplayName(request.displayName());
            profile.setPhoneNumber(request.phoneNumber());
        }

        // saveAndFlush forces the INSERT now so a UNIQUE violation surfaces as a
        // DataIntegrityViolationException here rather than later at commit time.
        return repository.saveAndFlush(profile);
    }

    private Jwt currentJwt() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("JWT principal expected but was: "
                + (principal == null ? "null" : principal.getClass().getName()));
    }
}
