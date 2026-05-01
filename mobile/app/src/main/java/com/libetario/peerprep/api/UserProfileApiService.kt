package com.libetario.peerprep.api

import com.libetario.peerprep.model.UpdateUserProfileRequest
import com.libetario.peerprep.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserProfileApiService {
    @GET("api/users/profile")
    suspend fun getProfile(@Query("email") email: String): Response<UserProfile>

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body request: UpdateUserProfileRequest): Response<UserProfile>
}