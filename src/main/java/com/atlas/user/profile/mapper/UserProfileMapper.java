package com.atlas.user.profile.mapper;

import com.atlas.user.profile.dto.UserProfileResponse;
import com.atlas.user.profile.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps UserProfile entities to API response DTOs (the entity {@code id} is exposed as {@code userId}). */
@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "userId", source = "id")
    UserProfileResponse toResponse(UserProfile profile);
}
