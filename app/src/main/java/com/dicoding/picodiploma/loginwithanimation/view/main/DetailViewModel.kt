package com.dicoding.picodiploma.loginwithanimation.view.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dicoding.picodiploma.loginwithanimation.data.StoryRepository
import com.dicoding.picodiploma.loginwithanimation.data.UserRepository
import com.dicoding.picodiploma.loginwithanimation.data.api.ListStoryItem
import com.dicoding.picodiploma.loginwithanimation.data.api.StoryDetailResponse
import com.dicoding.picodiploma.loginwithanimation.di.Injection

class StoryDetailViewModel(private val repository: StoryRepository) : ViewModel() {
    private val _storyDetail = MutableLiveData<StoryDetailResponse>()
    val storyDetail: LiveData<StoryDetailResponse> get() = _storyDetail

    fun fetchStoryDetail(storyId: String) {
        repository.getStoryDetail(storyId).observeForever { story ->
            if (story != null) {
                Log.d("StoryDetailViewModel", "Story detail updated: $story")
                _storyDetail.value = story
            } else {
                Log.e("StoryDetailViewModel", "Story detail is null")
            }
        }
    }
}

class StoryDetailViewModelFactory(private val storyRepository: StoryRepository) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StoryDetailViewModel(storyRepository) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: StoryDetailViewModelFactory? = null
        @JvmStatic
        fun getInstance(context: Context): StoryDetailViewModelFactory {
            if (INSTANCE == null) {
                synchronized(StoryDetailViewModelFactory::class.java) {
                    INSTANCE = StoryDetailViewModelFactory(
                        Injection.provideStoryRepository(context)
                    )
                }
            }
            return INSTANCE as StoryDetailViewModelFactory
        }
    }
}