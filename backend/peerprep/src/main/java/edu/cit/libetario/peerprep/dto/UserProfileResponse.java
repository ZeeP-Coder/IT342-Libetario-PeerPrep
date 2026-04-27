package edu.cit.libetario.peerprep.dto;

public record UserProfileResponse(
        String fullName,
        String email,
        String university,
        String major,
        boolean googleAuth) {
}
