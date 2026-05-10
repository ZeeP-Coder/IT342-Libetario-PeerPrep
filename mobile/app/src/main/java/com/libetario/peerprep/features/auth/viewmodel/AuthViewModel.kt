package com.libetario.peerprep.features.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libetario.peerprep.features.auth.model.AuthResponse
import com.libetario.peerprep.features.auth.model.LoginRequest
import com.libetario.peerprep.features.auth.model.RegisterRequest
import com.libetario.peerprep.features.auth.repository.AuthRepository
import com.libetario.peerprep.shared.util.Resource
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _loginStatus = MutableLiveData<Resource<AuthResponse>>()
    val loginStatus: LiveData<Resource<AuthResponse>> = _loginStatus

    private val _registerStatus = MutableLiveData<Resource<AuthResponse>>()
    val registerStatus: LiveData<Resource<AuthResponse>> = _registerStatus

    private val _googleLoginStatus = MutableLiveData<Resource<AuthResponse>>()
    val googleLoginStatus: LiveData<Resource<AuthResponse>> = _googleLoginStatus

    fun login(email: String, password: String) {
        _loginStatus.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val response = repository.login(LoginRequest(email, password))
                handleResponse(response, _loginStatus)
            } catch (e: Exception) {
                _loginStatus.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    fun register(name: String, email: String, university: String, major: String, password: String) {
        _registerStatus.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val response = repository.register(RegisterRequest(name, email, password, university, major))
                handleResponse(response, _registerStatus)
            } catch (e: Exception) {
                _registerStatus.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    fun googleLogin(idToken: String) {
        _googleLoginStatus.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val response = repository.googleLogin(idToken)
                handleResponse(response, _googleLoginStatus)
            } catch (e: Exception) {
                _googleLoginStatus.value = Resource.Error("Google login error: ${e.message}")
            }
        }
    }

    private fun handleResponse(response: retrofit2.Response<AuthResponse>, liveData: MutableLiveData<Resource<AuthResponse>>) {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                liveData.value = Resource.Success(body)
            } else {
                val msg = body?.message ?: "Login failed"
                liveData.value = Resource.Error(msg)
            }
        } else {
            val errorMsg = parseError(response.errorBody()?.string())
            liveData.value = Resource.Error(errorMsg)
        }
    }

    private fun parseError(errorJson: String?): String {
        return try {
            val obj = JSONObject(errorJson ?: "")
            obj.optString("message", obj.optString("error", "An unknown error occurred"))
        } catch (e: Exception) {
            "An unknown error occurred"
        }
    }
}