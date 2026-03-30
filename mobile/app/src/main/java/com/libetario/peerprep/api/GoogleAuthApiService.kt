package com.libetario.peerprep.api

import com.libetario.peerprep.model.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Backend endpoint you’ll add/confirm server-side:
 * POST /api/auth/google { "idToken": "..." }
 */
interface GoogleAuthApiService {
    @POST("api/auth/google")
    suspend fun googleLogin(@Body body: Map<String, String>): Response<AuthResponse>
}

