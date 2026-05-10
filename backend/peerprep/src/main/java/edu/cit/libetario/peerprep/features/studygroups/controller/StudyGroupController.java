package edu.cit.libetario.peerprep.features.studygroups.controller;

import edu.cit.libetario.peerprep.features.studygroups.dto.OperationResponse;
import edu.cit.libetario.peerprep.features.studygroups.dto.StudyGroupCreateRequest;
import edu.cit.libetario.peerprep.features.studygroups.dto.StudyGroupDashboardResponse;
import edu.cit.libetario.peerprep.features.studygroups.dto.StudyGroupJoinRequest;
import edu.cit.libetario.peerprep.features.studygroups.dto.StudyGroupResponse;
import edu.cit.libetario.peerprep.features.studygroups.service.StudyGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study-groups")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    public StudyGroupController(StudyGroupService studyGroupService) {
        this.studyGroupService = studyGroupService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<StudyGroupDashboardResponse> dashboard(@RequestParam String userEmail) {
        return ResponseEntity.ok(studyGroupService.getDashboard(userEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyGroupResponse> getStudyGroup(
            @PathVariable Long id,
            @RequestParam String userEmail) {
        return ResponseEntity.ok(studyGroupService.getStudyGroup(id, userEmail));
    }

    @PostMapping
    public ResponseEntity<OperationResponse> createStudyGroup(@Valid @RequestBody StudyGroupCreateRequest request) {
        OperationResponse response = studyGroupService.createStudyGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<OperationResponse> joinStudyGroup(
            @PathVariable Long id,
            @Valid @RequestBody StudyGroupJoinRequest request) {
        return ResponseEntity.ok(studyGroupService.joinStudyGroup(id, request));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<OperationResponse> leaveStudyGroup(
            @PathVariable Long id,
            @Valid @RequestBody StudyGroupJoinRequest request) {
        return ResponseEntity.ok(studyGroupService.leaveStudyGroup(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OperationResponse> deleteStudyGroup(
            @PathVariable Long id,
            @RequestParam String userEmail) {
        return ResponseEntity.ok(studyGroupService.deleteStudyGroup(id, userEmail));
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<OperationResponse> deleteStudyGroupByPost(
            @PathVariable Long id,
            @Valid @RequestBody StudyGroupJoinRequest request) {
        return ResponseEntity.ok(studyGroupService.deleteStudyGroup(id, request.userEmail()));
    }
}