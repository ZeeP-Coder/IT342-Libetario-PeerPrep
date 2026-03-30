package com.libetario.peerprep.model

data class AuthResponse(
    val message: String? = null,
    val user: User? = null,
    val token: String? = null,
    val success: Boolean? = null
)

data class User(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null
)

