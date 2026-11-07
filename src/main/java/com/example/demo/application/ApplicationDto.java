package com.example.demo.application;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Locale;
import org.hibernate.validator.constraints.URL;

public final class ApplicationDto {
    private ApplicationDto() { }
    public record Request(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @URL(regexp = "https?://.*") @Size(max = 2000) String cvUrl) {
        public Request {
            if (email != null) { email = email.trim().toLowerCase(Locale.ROOT); }
        }
    }
    public record Response(Long id, Long jobId, String fullName, String email, String cvUrl, Instant appliedAt) {
        public static Response from(JobApplication application) {
            return new Response(application.getId(), application.getJob().getId(), application.getFullName(),
                application.getEmail(), application.getCvUrl(), application.getAppliedAt());
        }
    }
}
