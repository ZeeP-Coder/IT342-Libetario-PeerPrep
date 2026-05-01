package com.libetario.peerprep.model

data class UserProfile(
    val fullName: String,
    val email: String,
    val university: String,
    val major: String,
    val googleAuth: Boolean
)

data class UpdateUserProfileRequest(
    val email: String,
    val fullName: String,
    val university: String,
    val major: String
)