package com.dicoding.picodiploma.loginwithanimation.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.dicoding.picodiploma.loginwithanimation.data.api.ApiConfig
import com.dicoding.picodiploma.loginwithanimation.data.api.ApiService
import com.dicoding.picodiploma.loginwithanimation.data.api.ListStoryItem
import com.dicoding.picodiploma.loginwithanimation.data.api.RegisterResponse
import com.dicoding.picodiploma.loginwithanimation.data.api.StoryDetailResponse
import com.dicoding.picodiploma.loginwithanimation.data.api.StoryResponse
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StoryRepository private constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreference
) {

   /*  fun getStories(): Call<StoryResponse> {
        return apiService.getStories()
    } */

    suspend fun getStories() : StoryResponse{
        return apiService.getStories()
    }

    fun getStoryDetail(storyId: String): LiveData<StoryDetailResponse> {
        val data = MutableLiveData<StoryDetailResponse>()
        apiService.getDetails(storyId)
            .enqueue(object : Callback<StoryDetailResponse> {
            override fun onResponse(call: Call<StoryDetailResponse>, response: Response<StoryDetailResponse>) {
                if (response.isSuccessful) {
                    data.value = response.body()
                    Log.d("detail activity", "Load story detail success!")
                }
            }

            override fun onFailure(call: Call<StoryDetailResponse>, t: Throwable) {
                Log.d("detail activity", "Failed to load story detail")
            }
        })
        return data
    }


    companion object {
        @Volatile
        private var instance: StoryRepository? = null

        fun getInstance(apiService: ApiService, userPreferences: UserPreference): StoryRepository =
            instance ?: synchronized(this) {
                instance ?: StoryRepository(apiService, userPreferences)
            }.also { instance = it }
    }
}