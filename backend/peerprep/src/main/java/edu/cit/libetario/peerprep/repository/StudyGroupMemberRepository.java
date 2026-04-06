package edu.cit.libetario.peerprep.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.cit.libetario.peerprep.entity.StudyGroupMember;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {
    List<StudyGroupMember> findByUserId(Long userId);

    List<StudyGroupMember> findByStudyGroupIdIn(Collection<Long> studyGroupIds);

    Optional<StudyGroupMember> findByStudyGroupIdAndUserId(Long studyGroupId, Long userId);

    boolean existsByStudyGroupIdAndUserId(Long studyGroupId, Long userId);

    @Transactional
    void deleteByStudyGroupId(Long studyGroupId);
}
