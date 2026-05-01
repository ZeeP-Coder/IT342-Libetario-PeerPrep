package com.libetario.peerprep.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /** 
     * Port 8081 matches your Spring Boot backend configuration.
     * Use 10.0.2.2 for Android Emulator to reach your PC's localhost.
     */
    private const val BASE_URL = "http://10.0.2.2:8081/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val googleAuthService: GoogleAuthApiService = retrofit.create(GoogleAuthApiService::class.java)
    val studyGroupService: StudyGroupApiService = retrofit.create(StudyGroupApiService::class.java)
    val userProfileService: UserProfileApiService = retrofit.create(UserProfileApiService::class.java)
}
