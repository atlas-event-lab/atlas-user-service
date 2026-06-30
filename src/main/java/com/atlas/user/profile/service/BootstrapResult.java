package com.atlas.user.profile.service;

import com.atlas.user.profile.dto.UserProfileResponse;

/**
 * Outcome of a bootstrap call. {@code created} distinguishes a first-time provisioning
 * (HTTP 201) from an idempotent return of an existing profile (HTTP 200).
 */
public record BootstrapResult(UserProfileResponse profile, boolean created) {}
