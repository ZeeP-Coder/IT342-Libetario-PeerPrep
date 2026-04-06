package edu.cit.libetario.peerprep.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StudyGroupResponse(
        Long id,
        String subject,
        String description,
        String day,
        String meetingTime,
        String location,
        Integer maxMembers,
        Integer currentMembers,
        String status,
        boolean joined,
        boolean ownedByCurrentUser,
        boolean joinable,
        String createdByName,
        String createdByEmail,
        LocalDateTime createdAt,
        List<String> memberNames) {
}
