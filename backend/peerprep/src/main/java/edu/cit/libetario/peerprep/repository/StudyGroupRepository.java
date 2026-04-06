package edu.cit.libetario.peerprep.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.cit.libetario.peerprep.entity.StudyGroup;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    List<StudyGroup> findAllByOrderByCreatedAtDesc();
}
