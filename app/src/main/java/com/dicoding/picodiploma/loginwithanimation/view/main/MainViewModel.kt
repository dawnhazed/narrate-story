package com.dicoding.picodiploma.loginwithanimation.view.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dicoding.picodiploma.loginwithanimation.data.QuoteRepository
import com.dicoding.picodiploma.loginwithanimation.data.StoryRepository
import com.dicoding.picodiploma.loginwithanimation.data.UserRepository
import com.dicoding.picodiploma.loginwithanimation.data.api.ListStoryItem
import com.dicoding.picodiploma.loginwithanimation.data.api.StoryResponse
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserModel
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserPreference
import com.dicoding.picodiploma.loginwithanimation.data.pref.dataStore
import com.dicoding.picodiploma.loginwithanimation.di.Injection
import com.dicoding.picodiploma.loginwithanimation.view.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: UserRepository,
    private val storyRepository: StoryRepository,
    quoteRepository: QuoteRepository,
    private val pref: UserPreference
    ) : ViewModel() {

    private val _stories = MutableLiveData<List<ListStoryItem>>()
    val stories: LiveData<List<ListStoryItem>> = _stories
    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    val quote: LiveData<PagingData<ListStoryItem>> =
        quoteRepository.getStories().cachedIn(viewModelScope)

    init {
        // checkLogin()
        getStories() }

    fun getSession(): LiveData<UserModel> {
        return repository.getSession().asLiveData()
    }

    /* fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    } */

    suspend fun checkLogin() : UserModel{
        return pref.getSession().first()
        /* viewModelScope.launch {
            val session = pref.getSession().first()
            _isLoggedIn.value = session.isLogin
        } */
    }

    private fun getStories() {
        viewModelScope.launch {
            try {
                val response = storyRepository.getStories()
                if (response != null && response.listStory != null) {
                    _stories.value = response.listStory
                } else {
                    _stories.value = emptyList()
                    // Log an error or handle the null case appropriately
                }
            } catch (e: Exception) {
                _stories.value = emptyList()
                // Log the exception or handle the error appropriately
            }
        }
    }

}

class StoryViewModelFactory(
    private val repository: UserRepository,
    private val storyRepository: StoryRepository,
    private val quoteRepository: QuoteRepository,
    private val pref: UserPreference
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repository, storyRepository, quoteRepository, pref) as T
    }

    companion object {
        @Volatile
        private var INSTANCE: StoryViewModelFactory? = null
        @JvmStatic
        fun getInstance(context: Context): StoryViewModelFactory {
            if (INSTANCE == null) {
                synchronized(StoryViewModelFactory::class.java) {
                    INSTANCE = StoryViewModelFactory(
                        Injection.provideRepository(context),
                        Injection.provideStoryRepository(context),
                        Injection.provideQuoteRepository(context),
                        UserPreference.getInstance(context.dataStore)
                    )
                }
            }
            return INSTANCE as StoryViewModelFactory
        }
    }
}