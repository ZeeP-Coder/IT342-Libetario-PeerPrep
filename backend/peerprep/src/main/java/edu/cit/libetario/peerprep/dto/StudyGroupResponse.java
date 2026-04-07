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

        public static Builder builder() {
                return new Builder();
        }

        public static final class Builder {
                private Long id;
                private String subject;
                private String description;
                private String day;
                private String meetingTime;
                private String location;
                private Integer maxMembers;
                private Integer currentMembers;
                private String status;
                private boolean joined;
                private boolean ownedByCurrentUser;
                private boolean joinable;
                private String createdByName;
                private String createdByEmail;
                private LocalDateTime createdAt;
                private List<String> memberNames;

                public Builder id(Long id) {
                        this.id = id;
                        return this;
                }

                public Builder subject(String subject) {
                        this.subject = subject;
                        return this;
                }

                public Builder description(String description) {
                        this.description = description;
                        return this;
                }

                public Builder day(String day) {
                        this.day = day;
                        return this;
                }

                public Builder meetingTime(String meetingTime) {
                        this.meetingTime = meetingTime;
                        return this;
                }

                public Builder location(String location) {
                        this.location = location;
                        return this;
                }

                public Builder maxMembers(Integer maxMembers) {
                        this.maxMembers = maxMembers;
                        return this;
                }

                public Builder currentMembers(Integer currentMembers) {
                        this.currentMembers = currentMembers;
                        return this;
                }

                public Builder status(String status) {
                        this.status = status;
                        return this;
                }

                public Builder joined(boolean joined) {
                        this.joined = joined;
                        return this;
                }

                public Builder ownedByCurrentUser(boolean ownedByCurrentUser) {
                        this.ownedByCurrentUser = ownedByCurrentUser;
                        return this;
                }

                public Builder joinable(boolean joinable) {
                        this.joinable = joinable;
                        return this;
                }

                public Builder createdByName(String createdByName) {
                        this.createdByName = createdByName;
                        return this;
                }

                public Builder createdByEmail(String createdByEmail) {
                        this.createdByEmail = createdByEmail;
                        return this;
                }

                public Builder createdAt(LocalDateTime createdAt) {
                        this.createdAt = createdAt;
                        return this;
                }

                public Builder memberNames(List<String> memberNames) {
                        this.memberNames = memberNames;
                        return this;
                }

                public StudyGroupResponse build() {
                        return new StudyGroupResponse(
                                        id,
                                        subject,
                                        description,
                                        day,
                                        meetingTime,
                                        location,
                                        maxMembers,
                                        currentMembers,
                                        status,
                                        joined,
                                        ownedByCurrentUser,
                                        joinable,
                                        createdByName,
                                        createdByEmail,
                                        createdAt,
                                        memberNames);
                }
        }
}
