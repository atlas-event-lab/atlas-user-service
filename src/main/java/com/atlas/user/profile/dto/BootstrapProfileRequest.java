package com.atlas.user.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Optional seed fields for first-time provisioning (user.yaml — BootstrapProfileRequest).
 * Identity (userId) and email are taken from the JWT claims, never from this body (SEC-004).
 * These fields are applied only when the profile is created (create-only seed).
 */
public record BootstrapProfileRequest(

        @Size(max = 100)
        String displayName,

        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "must be E.164 format")
        String phoneNumber
) {}
