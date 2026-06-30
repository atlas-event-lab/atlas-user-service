package com.atlas.user.profile.service;

import com.atlas.user.profile.dto.BootstrapProfileRequest;

public interface ProfileService {

    /**
     * Idempotent Just-In-Time provisioning of the caller's profile (SPEC-FEATURE-BOOTSTRAP-PROFILE).
     * Identity (keycloakUserId, email, names) is read from the validated JWT, never from the
     * request (SEC-004). On first call it creates the profile from the claims (created=true);
     * on later calls it returns the existing profile unchanged (created=false). The optional
     * {@code displayName}/{@code phoneNumber} are applied only on creation (create-only seed).
     */
    BootstrapResult bootstrap(BootstrapProfileRequest request);
}
