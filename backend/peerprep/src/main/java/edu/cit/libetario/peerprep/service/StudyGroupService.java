package edu.cit.libetario.peerprep.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.cit.libetario.peerprep.dto.OperationResponse;
import edu.cit.libetario.peerprep.dto.StudyGroupCreateRequest;
import edu.cit.libetario.peerprep.dto.StudyGroupDashboardResponse;
import edu.cit.libetario.peerprep.dto.StudyGroupJoinRequest;
import edu.cit.libetario.peerprep.dto.StudyGroupResponse;
import edu.cit.libetario.peerprep.dto.StudyPartnerResponse;
import edu.cit.libetario.peerprep.entity.StudyGroup;
import edu.cit.libetario.peerprep.entity.StudyGroupMember;
import edu.cit.libetario.peerprep.entity.User;
import edu.cit.libetario.peerprep.repository.StudyGroupMemberRepository;
import edu.cit.libetario.peerprep.repository.StudyGroupRepository;
import edu.cit.libetario.peerprep.repository.UserRepository;

@Service
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final UserRepository userRepository;

    public StudyGroupService(
            StudyGroupRepository studyGroupRepository,
            StudyGroupMemberRepository studyGroupMemberRepository,
            UserRepository userRepository) {
        this.studyGroupRepository = studyGroupRepository;
        this.studyGroupMemberRepository = studyGroupMemberRepository;
        this.userRepository = userRepository;
    }

    public StudyGroupDashboardResponse getDashboard(String userEmail) {
        User currentUser = findUserByEmail(userEmail);
        List<StudyGroup> groups = studyGroupRepository.findAllByOrderByCreatedAtDesc();

        if (groups.isEmpty()) {
            return new StudyGroupDashboardResponse(
                    currentUser.getFullName(),
                    currentUser.getEmail(),
                    0,
                    0,
                    0,
                    0,
                    null,
                    List.of(),
                    List.of(),
                    List.of());
        }

        Map<Long, List<StudyGroupMember>> membersByGroupId = loadMembersByGroupId(groups);
        Set<Long> joinedGroupIds = membersByGroupId.values().stream()
                .flatMap(List::stream)
                .filter(member -> member.getUser().getId().equals(currentUser.getId()))
                .map(member -> member.getStudyGroup().getId())
                .collect(Collectors.toSet());

        List<StudyGroupResponse> allResponses = groups.stream()
                .map(group -> toResponse(group, currentUser, membersByGroupId.getOrDefault(group.getId(), List.of()), joinedGroupIds.contains(group.getId())))
                .toList();

        List<StudyGroupResponse> availableGroups = allResponses.stream()
                .filter(group -> !group.joined() && group.joinable())
                .toList();

        List<StudyGroupResponse> myGroups = allResponses.stream()
                .filter(StudyGroupResponse::joined)
                .toList();

        List<StudyPartnerResponse> partners = buildPartners(currentUser, joinedGroupIds, membersByGroupId);

        return new StudyGroupDashboardResponse(
                currentUser.getFullName(),
                currentUser.getEmail(),
                allResponses.size(),
                availableGroups.size(),
                myGroups.size(),
                partners.size(),
                pickNextSession(myGroups),
                availableGroups,
                myGroups,
                partners);
    }

    public StudyGroupResponse getStudyGroup(Long groupId, String userEmail) {
        User currentUser = findUserByEmail(userEmail);
        StudyGroup studyGroup = findGroupById(groupId);
        List<StudyGroupMember> members = studyGroupMemberRepository.findByStudyGroupIdIn(List.of(groupId));
        boolean joined = members.stream().anyMatch(member -> member.getUser().getId().equals(currentUser.getId()));
        return toResponse(studyGroup, currentUser, members, joined);
    }

    public OperationResponse createStudyGroup(StudyGroupCreateRequest request) {
        User creator = findUserByEmail(request.creatorEmail());

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setSubject(request.subject().trim());
        studyGroup.setDescription(request.description().trim());
        studyGroup.setDay(request.day().trim());
        studyGroup.setMeetingTime(request.meetingTime().trim());
        studyGroup.setLocation(request.location().trim());
        studyGroup.setMaxMembers(request.maxMembers());
        studyGroup.setCreator(creator);
        studyGroup.setCreatedAt(LocalDateTime.now());

        StudyGroup savedGroup = studyGroupRepository.save(studyGroup);

        StudyGroupMember creatorMembership = new StudyGroupMember();
        creatorMembership.setStudyGroup(savedGroup);
        creatorMembership.setUser(creator);
        creatorMembership.setJoinedAt(LocalDateTime.now());
        studyGroupMemberRepository.save(creatorMembership);

        return new OperationResponse(true, "Study group created successfully");
    }

    public OperationResponse joinStudyGroup(Long groupId, StudyGroupJoinRequest request) {
        User user = findUserByEmail(request.userEmail());
        StudyGroup studyGroup = findGroupById(groupId);

        if (studyGroupMemberRepository.existsByStudyGroupIdAndUserId(groupId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this study group");
        }

        int currentMembers = getMemberCount(groupId);
        if (currentMembers >= studyGroup.getMaxMembers()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This study group is already full");
        }

        StudyGroupMember membership = new StudyGroupMember();
        membership.setStudyGroup(studyGroup);
        membership.setUser(user);
        membership.setJoinedAt(LocalDateTime.now());
        studyGroupMemberRepository.save(membership);

        return new OperationResponse(true, "You joined the study group successfully");
    }

    public OperationResponse leaveStudyGroup(Long groupId, StudyGroupJoinRequest request) {
        User user = findUserByEmail(request.userEmail());
        StudyGroup studyGroup = findGroupById(groupId);

        if (studyGroup.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Creators cannot leave their own group. Delete the group instead");
        }

        StudyGroupMember membership = studyGroupMemberRepository.findByStudyGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this study group"));

        studyGroupMemberRepository.delete(membership);
        return new OperationResponse(true, "You left the study group");
    }

    public OperationResponse deleteStudyGroup(Long groupId, String userEmail) {
        StudyGroup studyGroup = findGroupById(groupId);
        String normalizedEmail = userEmail == null ? "" : userEmail.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User email is required");
        }

        if (!studyGroup.getCreator().getEmail().trim().toLowerCase(Locale.ROOT).equals(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the study group creator can delete this group");
        }

        studyGroupMemberRepository.deleteByStudyGroupId(groupId);
        studyGroupRepository.delete(studyGroup);

        return new OperationResponse(true, "Study group deleted successfully");
    }

    private User findUserByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private StudyGroup findGroupById(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study group not found"));
    }

    private Map<Long, List<StudyGroupMember>> loadMembersByGroupId(List<StudyGroup> groups) {
        List<Long> groupIds = groups.stream().map(StudyGroup::getId).toList();
        List<StudyGroupMember> members = groupIds.isEmpty() ? List.of() : studyGroupMemberRepository.findByStudyGroupIdIn(groupIds);
        Map<Long, List<StudyGroupMember>> membersByGroupId = new HashMap<>();
        for (StudyGroupMember member : members) {
            membersByGroupId.computeIfAbsent(member.getStudyGroup().getId(), key -> new ArrayList<>()).add(member);
        }
        return membersByGroupId;
    }

    private StudyGroupResponse toResponse(
            StudyGroup studyGroup,
            User currentUser,
            List<StudyGroupMember> members,
            boolean joined) {
        int currentMembers = members.size();
        boolean joinable = currentMembers < studyGroup.getMaxMembers();
        String status = joinable ? "Open" : "Full";

        return new StudyGroupResponse(
                studyGroup.getId(),
                studyGroup.getSubject(),
                studyGroup.getDescription(),
                studyGroup.getDay(),
                studyGroup.getMeetingTime(),
                studyGroup.getLocation(),
                studyGroup.getMaxMembers(),
                currentMembers,
                status,
                joined,
                studyGroup.getCreator().getId().equals(currentUser.getId()),
                joinable && !joined,
                studyGroup.getCreator().getFullName(),
                studyGroup.getCreator().getEmail(),
                studyGroup.getCreatedAt(),
                members.stream()
                    .map(member -> member.getUser().getFullName())
                    .distinct()
                    .sorted()
                    .toList());
    }

    private List<StudyPartnerResponse> buildPartners(
            User currentUser,
            Set<Long> joinedGroupIds,
            Map<Long, List<StudyGroupMember>> membersByGroupId) {
        if (joinedGroupIds.isEmpty()) {
            return List.of();
        }

        Map<String, PartnerAccumulator> partnerMap = new LinkedHashMap<>();
        for (Long groupId : joinedGroupIds) {
            List<StudyGroupMember> members = membersByGroupId.getOrDefault(groupId, List.of());
            for (StudyGroupMember member : members) {
                User partner = member.getUser();
                if (partner.getId().equals(currentUser.getId())) {
                    continue;
                }

                partnerMap.computeIfAbsent(partner.getEmail(), key -> new PartnerAccumulator(partner)).sharedGroups++;
            }
        }

        return partnerMap.values().stream()
                .sorted(Comparator.comparingInt((PartnerAccumulator partner) -> partner.sharedGroups).reversed()
                        .thenComparing(partner -> partner.user.getFullName()))
                .map(partner -> new StudyPartnerResponse(
                        partner.user.getFullName(),
                        partner.user.getEmail(),
                        partner.user.getUniversity(),
                        partner.user.getMajor(),
                        partner.sharedGroups))
                .toList();
    }

    private StudyGroupResponse pickNextSession(List<StudyGroupResponse> myGroups) {
        if (myGroups.isEmpty()) {
            return null;
        }

        return myGroups.stream()
                .sorted(Comparator.comparingInt((StudyGroupResponse group) -> dayOrder(group.day()))
                        .thenComparing(StudyGroupResponse::meetingTime)
                        .thenComparing(StudyGroupResponse::createdAt))
                .findFirst()
                .orElse(myGroups.getFirst());
    }

    private int dayOrder(String day) {
        return switch (day.trim().toLowerCase(Locale.ROOT)) {
            case "monday" -> 1;
            case "tuesday" -> 2;
            case "wednesday" -> 3;
            case "thursday" -> 4;
            case "friday" -> 5;
            case "saturday" -> 6;
            case "sunday" -> 7;
            default -> 8;
        };
    }

    private int getMemberCount(Long groupId) {
        return studyGroupMemberRepository.findByStudyGroupIdIn(List.of(groupId)).size();
    }

    private static final class PartnerAccumulator {
        private final User user;
        private int sharedGroups;

        private PartnerAccumulator(User user) {
            this.user = user;
        }
    }
}
