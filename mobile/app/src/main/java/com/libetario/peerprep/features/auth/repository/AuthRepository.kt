package com.libetario.peerprep.features.auth.repository

import com.libetario.peerprep.features.auth.api.GoogleLoginRequest
import com.libetario.peerprep.features.auth.model.AuthResponse
import com.libetario.peerprep.features.auth.model.LoginRequest
import com.libetario.peerprep.features.auth.model.RegisterRequest
import com.libetario.peerprep.shared.api.RetrofitClient
import retrofit2.Response

class AuthRepository {
    private val api = RetrofitClient.authService
    private val googleApi = RetrofitClient.googleAuthService

    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return api.login(request)
    }

    suspend fun register(request: RegisterRequest): Response<AuthResponse> {
        return api.register(request)
    }

    suspend fun googleLogin(idToken: String): Response<AuthResponse> {
        return googleApi.googleLogin(GoogleLoginRequest(idToken))
    }
}