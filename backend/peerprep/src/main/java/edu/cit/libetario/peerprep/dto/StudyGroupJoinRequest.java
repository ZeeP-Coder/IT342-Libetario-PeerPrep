package edu.cit.libetario.peerprep.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudyGroupJoinRequest(@NotBlank @Email String userEmail) {
}
