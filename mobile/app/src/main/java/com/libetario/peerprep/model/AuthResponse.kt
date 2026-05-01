package com.libetario.peerprep.model

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)
