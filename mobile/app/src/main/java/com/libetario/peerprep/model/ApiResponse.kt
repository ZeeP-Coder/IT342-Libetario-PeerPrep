package com.libetario.peerprep.model

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val timestamp: String? = null
)

data class ApiError(
    val code: String? = null,
    val message: String? = null,
    val details: Any? = null
)
