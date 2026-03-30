package com.libetario.peerprep.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Update this to your backend URL
    // For Android emulator: http://10.0.2.2:8081
    // For physical device: http://your-ip:8081
    private const val BASE_URL = "http://10.0.2.2:8081/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val googleAuthService: GoogleAuthApiService = retrofit.create(GoogleAuthApiService::class.java)
}
