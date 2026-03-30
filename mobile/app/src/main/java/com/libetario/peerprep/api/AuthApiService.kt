package com.libetario.peerprep.api

import com.libetario.peerprep.model.AuthResponse
import com.libetario.peerprep.model.LoginRequest
import com.libetario.peerprep.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}

