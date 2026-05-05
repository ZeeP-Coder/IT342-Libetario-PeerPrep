package com.libetario.peerprep.features.studygroups.api

import com.libetario.peerprep.features.studygroups.model.JoinLeaveRequest
import com.libetario.peerprep.features.studygroups.model.StudyGroup
import com.libetario.peerprep.features.studygroups.model.StudyGroupDashboard
import com.libetario.peerprep.model.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StudyGroupApiService {
    @GET("api/study-groups/dashboard")
    suspend fun getDashboard(@Query("userEmail") email: String): Response<StudyGroupDashboard>

    @GET("api/study-groups/{id}")
    suspend fun getStudyGroup(
        @Path("id") groupId: Long,
        @Query("userEmail") userEmail: String
    ): Response<StudyGroup>

    @POST("api/study-groups/{id}/join")
    suspend fun joinStudyGroup(
        @Path("id") groupId: Long,
        @Body request: JoinLeaveRequest
    ): Response<ApiResponse<Any>>

    @POST("api/study-groups/{id}/leave")
    suspend fun leaveStudyGroup(
        @Path("id") groupId: Long,
        @Body request: JoinLeaveRequest
    ): Response<ApiResponse<Any>>

    @DELETE("api/study-groups/{id}")
    suspend fun deleteStudyGroup(
        @Path("id") groupId: Long,
        @Query("userEmail") userEmail: String
    ): Response<ApiResponse<Any>>

    @POST("api/study-groups")
    suspend fun createStudyGroup(
        @Body request: Any
    ): Response<ApiResponse<Any>>
}
