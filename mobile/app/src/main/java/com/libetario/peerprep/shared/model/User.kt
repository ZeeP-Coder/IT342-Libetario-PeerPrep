package com.libetario.peerprep.shared.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String? = null,
    val email: String? = null,
    @SerializedName("fullname", alternate = ["name", "fullName"])
    val name: String? = null,
    val university: String? = null,
    val major: String? = null,
    val role: String? = null,
    val googleAuth: Boolean = false
)
