package com.atlas.user.profile.repository;

import com.atlas.user.profile.entity.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /** Looks up a profile by its Keycloak identity (JWT {@code sub}). */
    Optional<UserProfile> findByKeycloakUserId(String keycloakUserId);
}
