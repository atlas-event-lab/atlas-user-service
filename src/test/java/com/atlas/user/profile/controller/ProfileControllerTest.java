package com.atlas.user.profile.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlas.user.profile.dto.UserProfileResponse;
import com.atlas.user.profile.entity.ProfileStatus;
import com.atlas.user.profile.service.BootstrapResult;
import com.atlas.user.profile.service.ProfileService;
import com.atlas.user.shared.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    ProfileService profileService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProfileController controller = new ProfileController(profileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserProfileResponse sampleProfile() {
        return new UserProfileResponse(
                UUID.randomUUID(),
                "user@atlas.local",
                "Ada",
                "Lovelace",
                null,
                null,
                ProfileStatus.ACTIVE,
                Instant.now(),
                Instant.now());
    }

    @Test
    void returns201WhenProfileCreated() throws Exception {
        when(profileService.bootstrap(any())).thenReturn(new BootstrapResult(sampleProfile(), true));

        mockMvc.perform(post("/api/v1/me/profile")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void returns200WhenProfileAlreadyExisted() throws Exception {
        when(profileService.bootstrap(any())).thenReturn(new BootstrapResult(sampleProfile(), false));

        mockMvc.perform(post("/api/v1/me/profile")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void returns400WhenPhoneNumberNotE164() throws Exception {
        mockMvc.perform(post("/api/v1/me/profile")
                        .contentType("application/json")
                        .content("{\"phoneNumber\":\"not-a-phone\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors[0].field").value("phoneNumber"));
    }
}
