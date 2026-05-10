package edu.cit.libetario.peerprep.features.studygroups.dto;

import java.util.List;

public record StudyGroupDashboardResponse(
        String currentUserName,
        String currentUserEmail,
        int activeGroups,
        int availableGroups,
        int myGroups,
        int partnerCount,
        StudyGroupResponse nextSession,
        List<StudyGroupResponse> availableStudyGroups,
        List<StudyGroupResponse> myStudyGroups,
        List<StudyPartnerResponse> studyPartners) {
}