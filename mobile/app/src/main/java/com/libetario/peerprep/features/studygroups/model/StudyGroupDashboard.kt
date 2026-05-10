package com.libetario.peerprep.features.studygroups.model

data class StudyGroupDashboard(
    val currentUserName: String,
    val currentUserEmail: String,
    val activeGroups: Int,
    val availableGroups: Int,
    val myGroups: Int,
    val partnerCount: Int,
    val nextSession: StudyGroup? = null,
    val availableStudyGroups: List<StudyGroup>,
    val myStudyGroups: List<StudyGroup>,
    val studyPartners: List<StudyPartner>
)
