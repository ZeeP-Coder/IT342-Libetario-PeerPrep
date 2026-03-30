package com.libetario.peerprep.model

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val university: String? = null,
    val major: String? = null
)

