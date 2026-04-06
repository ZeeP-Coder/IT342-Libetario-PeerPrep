package edu.cit.libetario.peerprep.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudyGroupCreateRequest(
        @NotBlank @Email String creatorEmail,
        @NotBlank @Size(min = 3, max = 80) String subject,
        @NotBlank @Size(min = 20, max = 500) String description,
        @NotBlank String day,
        @NotBlank String meetingTime,
        @NotBlank @Size(min = 3, max = 120) String location,
        @NotNull @Min(2) @Max(50) Integer maxMembers) {
}
