package com.libetario.peerprep.api

import com.libetario.peerprep.model.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class GoogleLoginRequest(
    val idToken: String
)

interface GoogleAuthApiService {
    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>
}
