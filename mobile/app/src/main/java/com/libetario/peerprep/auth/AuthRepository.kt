package com.libetario.peerprep.auth

import com.libetario.peerprep.api.RetrofitClient
import com.libetario.peerprep.api.GoogleLoginRequest
import com.libetario.peerprep.model.AuthResponse
import com.libetario.peerprep.model.LoginRequest
import com.libetario.peerprep.model.RegisterRequest
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
