package com.libetario.peerprep.model

data class StudyGroup(
    val id: Long,
    val subject: String,
    val description: String,
    val day: String,
    val meetingTime: String,
    val location: String,
    val currentMembers: Int,
    val maxMembers: Int,
    val createdByName: String,
    val createdByEmail: String,
    val status: String = "OPEN",
    val joined: Boolean = false,
    val joinable: Boolean = true,
    val ownedByCurrentUser: Boolean = false,
    val createdAt: String = "",
    val memberNames: List<String> = emptyList()
)
