package com.atlas.user.profile.dto;

import com.atlas.user.profile.entity.ProfileStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * The caller's profile (user.yaml — UserProfileResponse). {@code userId} is the
 * service-owned profile id (UserProfile.id), not the Keycloak sub.
 */
public record UserProfileResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String phoneNumber,
        ProfileStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
