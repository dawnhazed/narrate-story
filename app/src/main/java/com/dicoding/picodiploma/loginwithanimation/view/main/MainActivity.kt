package com.dicoding.picodiploma.loginwithanimation.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.picodiploma.loginwithanimation.R
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserPreference
import com.dicoding.picodiploma.loginwithanimation.data.pref.dataStore
import com.dicoding.picodiploma.loginwithanimation.databinding.ActivityMainBinding
import com.dicoding.picodiploma.loginwithanimation.view.ViewModelFactory
import com.dicoding.picodiploma.loginwithanimation.view.adapter.StoryAdapter
import com.dicoding.picodiploma.loginwithanimation.view.add.AddActivity
import com.dicoding.picodiploma.loginwithanimation.view.login.LoginActivity
import com.dicoding.picodiploma.loginwithanimation.view.welcome.WelcomeActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainViewModel> {
        StoryViewModelFactory.getInstance(this)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StoryAdapter
    private lateinit var pref: UserPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //toolbar
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        adapter = StoryAdapter()
        pref = UserPreference.getInstance(dataStore)

        binding.mainRv.layoutManager = LinearLayoutManager(this)
        binding.mainRv.adapter = adapter

        lifecycleScope.launch {
            if (!pref.getSession().first().isLogin) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
       /* viewModel.getSession().observe(this) { user ->
            if (!user.isLogin) {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        } */

        setupView()
        setupAction()
        getStories()
    }

    override fun onResume() {
        super.onResume()
        getStories()
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private fun setupAction() {
        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
            showLoading(true)
        }
    }

    private fun getStories() {
        showLoading(true)
        viewModel.stories.observe(this, Observer { stories ->
            adapter.submitList(stories)
            showLoading(false)
        })
        viewModel.getStories()
    }

    //logout

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout(){
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

}