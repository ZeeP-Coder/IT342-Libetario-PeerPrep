package com.libetario.peerprep.api

import com.libetario.peerprep.model.AuthResponse
import com.libetario.peerprep.model.LoginRequest
import com.libetario.peerprep.model.RegisterRequest
import com.libetario.peerprep.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @PUT("api/auth/profile/{email}")
    suspend fun updateProfile(
        @Path("email") email: String,
        @Body user: User
    ): Response<AuthResponse>
}
