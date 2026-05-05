package com.libetario.peerprep.features.auth.model

data class RegisterRequest(
    val fullname: String,
    val email: String,
    val password: String,
    val university: String? = null,
    val major: String? = null
)