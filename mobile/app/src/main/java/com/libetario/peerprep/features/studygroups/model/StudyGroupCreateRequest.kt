package com.libetario.peerprep.features.studygroups.model

data class StudyGroupCreateRequest(
    val creatorEmail: String,
    val subject: String,
    val description: String,
    val day: String,
    val meetingTime: String,
    val location: String,
    val maxMembers: Int
)
