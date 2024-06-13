package com.dicoding.picodiploma.loginwithanimation.view.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserModel
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserPreference
import com.dicoding.picodiploma.loginwithanimation.data.pref.dataStore
import com.dicoding.picodiploma.loginwithanimation.databinding.ActivityLoginBinding
import com.dicoding.picodiploma.loginwithanimation.view.ViewModelFactory
import com.dicoding.picodiploma.loginwithanimation.view.main.MainActivity
import com.dicoding.picodiploma.loginwithanimation.view.signup.SignupActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val viewModel by viewModels<LoginViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var binding: ActivityLoginBinding
    private lateinit var pref: UserPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pref = UserPreference.getInstance(dataStore)

        setupView()
        setupAction()
        observeViewModel()

        // viewmodel setup
        /* viewModel.loginResult.observe(this) { response ->
            showLoading(false)
            if (response?.loginResult?.token != null) {
                // Login successful, proceed to next screen
                saveToken(response.loginResult.token)
                showDialogue("Anda berhasil login. Sudah tidak sabar untuk belajar ya?")
            } else {
                Toast.makeText(
                    this,
                    "Email/Password yang Anda Masukkan Salah",
                    Toast.LENGTH_SHORT
                ).show()
                //Toast.makeText(this, response?.message, Toast.LENGTH_SHORT).show()
            }
        } */
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
        supportActionBar?.hide()
    }

    private fun setupAction() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            showLoading(true)
            viewModel.loginUser(email, password)

            Log.d("login button", "Login Button Clicked!")
        }

        binding.btnIntent.setOnClickListener{
            showLoading(true)
            startActivity(Intent(this, SignupActivity::class.java))
            showLoading(false)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { response ->
            if (response?.loginResult?.token != null) {
                val email = binding.emailEditText.text.toString()
                val token = response.loginResult.token
                lifecycleScope.launch {
                    pref.saveSession(UserModel(email, token, true))
                    showDialogue("Anda berhasil login. Sudah tidak sabar untuk belajar ya?")
                }
            } else {
                Toast.makeText(this, response?.message, Toast.LENGTH_SHORT).show()
            }
        }
        /* viewModel.loginResult.observe(this) { response ->
            showLoading(false)
            if (response?.error == false) {
                val email = binding.emailEditText.text.toString()
                val token = response.loginResult?.token
                viewModel.saveSession(UserModel(email, token))

            } else {
                Toast.makeText(
                    this,
                    "Email/Password yang Anda Masukkan Salah",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } */
    }

    private fun showDialogue(message:String) {
        AlertDialog.Builder(this).apply {
            setTitle("Yeah!")
            setMessage(message)
            setPositiveButton("Lanjut") { _, _ ->
                val intent = Intent(context, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            create()
            show()
        }

    }

    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.apply()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}


