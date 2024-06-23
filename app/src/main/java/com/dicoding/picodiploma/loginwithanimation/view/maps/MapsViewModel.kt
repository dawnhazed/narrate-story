package com.dicoding.picodiploma.loginwithanimation.view.maps

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.picodiploma.loginwithanimation.data.StoryRepository
import com.dicoding.picodiploma.loginwithanimation.data.api.ListStoryItem
import com.dicoding.picodiploma.loginwithanimation.data.api.Story
import kotlinx.coroutines.launch

class MapsViewModel(private val repository: StoryRepository) : ViewModel() {
    private val _stories = MutableLiveData<List<ListStoryItem>>()
    val stories: LiveData<List<ListStoryItem>> = _stories

    fun fetchStoriesWithLocation() {
        viewModelScope.launch {
            try {
                val response = repository.getLocation()
                _stories.value = response.listStory
                Log.d("map view model", "Successfully fetched stories with location: ${response.listStory.size}")
            } catch (e: Exception) {
                Log.d("map view model", "error fetching stories with loc")
            }
        }
    }
}