package com.libetario.peerprep.features.studygroups.model

data class StudyPartner(
    val fullName: String,
    val email: String,
    val university: String,
    val major: String,
    val sharedGroups: Int
)
