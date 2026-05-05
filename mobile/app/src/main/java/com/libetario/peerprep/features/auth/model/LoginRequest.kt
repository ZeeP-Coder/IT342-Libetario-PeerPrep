package com.libetario.peerprep.features.auth.model

data class LoginRequest(
    val email: String,
    val password: String
)