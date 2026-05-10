package edu.cit.libetario.peerprep.features.studygroups.dto;

public record StudyPartnerResponse(
        String fullName,
        String email,
        String university,
        String major,
        int sharedGroups) {
}