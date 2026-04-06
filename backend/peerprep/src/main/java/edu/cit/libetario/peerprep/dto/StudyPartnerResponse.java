package edu.cit.libetario.peerprep.dto;

public record StudyPartnerResponse(
        String fullName,
        String email,
        String university,
        String major,
        int sharedGroups) {
}
