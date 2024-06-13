package com.dicoding.picodiploma.loginwithanimation.view.signup

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dicoding.picodiploma.loginwithanimation.data.UserRepository
import com.dicoding.picodiploma.loginwithanimation.data.api.ApiConfig
import com.dicoding.picodiploma.loginwithanimation.data.api.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupViewModel (private val userRepository: UserRepository) : ViewModel() {

    private val _registerResult = MutableLiveData<RegisterResponse?>()
    val registerResult: LiveData<RegisterResponse?> = _registerResult

    fun registerUser(name: String, email: String, password: String) {
        ApiConfig.getApiService("").register(name, email, password)
            .enqueue(object : Callback<RegisterResponse> {

                override fun onResponse(
                    call: Call<RegisterResponse>,
                    response: Response<RegisterResponse>
                ) {
                    if (response.isSuccessful) {
                        val registerResponse = response.body()
                        registerResponse?.let {
                            _registerResult.value = it
                            Log.d("register user", "Registration Successful : ${it.message}")
                        }
                    } else {
                        _registerResult.value = null
                        Log.d("register user", "Registration Failed")
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    _registerResult.value = null
                    Log.d("register user", "Registration Error")
                }

            })
    }
}