package com.atlas.user.profile.controller;

import com.atlas.user.profile.dto.BootstrapProfileRequest;
import com.atlas.user.profile.dto.UserProfileResponse;
import com.atlas.user.profile.service.BootstrapResult;
import com.atlas.user.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Profile API (contracts/openapi/user.yaml). Holds no business logic
 * (API-003); delegates entirely to {@link ProfileService}. JWT is required by SecurityConfig.
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * POST /me/profile — Idempotent bootstrap of the caller's profile. Returns 201 when the
     * profile is created on first call, 200 when an existing profile is returned unchanged.
     * The body is optional; identity comes from the JWT, never the body (SEC-004).
     */
    @PostMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> bootstrapMyProfile(
            @RequestBody(required = false) @Valid BootstrapProfileRequest request) {

        BootstrapResult result = profileService.bootstrap(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.profile());
    }
}
