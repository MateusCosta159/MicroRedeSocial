package com.mateus.microredesocial.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mateus.microredesocial.R
import com.mateus.microredesocial.auth.UserAuth
import com.mateus.microredesocial.dao.UserDao
import com.mateus.microredesocial.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = UserAuth()
    private val userDao = UserDao()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom); insets
        }

        if (auth.isLoggedIn()) { goToHome(); return }
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { if (validateFields()) loginUser() }
        binding.btnCreateUser.setOnClickListener { startActivity(Intent(this, SignUpActivity::class.java)) }
        binding.txtForgotPassword.setOnClickListener { sendPasswordReset() }
    }

    private fun validateFields(): Boolean {
        val input = binding.edtEmail.text.toString().trim()
        val senha = binding.edtSenha.text.toString()
        binding.tilEmail.error = null; binding.tilSenha.error = null
        if (input.isEmpty()) { binding.tilEmail.error = getString(R.string.error_field_email_empty); return false }
        if (senha.isEmpty()) { binding.tilSenha.error = getString(R.string.error_field_password_empty); return false }
        return true
    }

    private fun loginUser() {
        val input = binding.edtEmail.text.toString().trim()
        val senha = binding.edtSenha.text.toString()
        setLoading(true)
        if (input.contains("@")) doLogin(input, senha)
        else resolveUsernameAndLogin(input, senha)
    }

    private fun resolveUsernameAndLogin(username: String, senha: String) {
        userDao.getUserByUsername(username) { user ->
            if (user == null) { setLoading(false); binding.tilEmail.error = getString(R.string.error_username_not_found); return@getUserByUsername }
            doLogin(user.email, senha)
        }
    }

    private fun doLogin(email: String, senha: String) {
        auth.login(email, senha) { sucesso, erro ->
            setLoading(false)
            if (sucesso) goToHome()
            else Toast.makeText(this, erro ?: getString(R.string.error_login), Toast.LENGTH_LONG).show()
        }
    }

    private fun sendPasswordReset() {
        val input = binding.edtEmail.text.toString().trim()
        if (input.isEmpty()) { binding.tilEmail.error = getString(R.string.error_field_email_reset); return }
        if (!input.contains("@")) {
            userDao.getUserByUsername(input) { user ->
                if (user == null) { binding.tilEmail.error = getString(R.string.error_username_not_found); return@getUserByUsername }
                sendReset(user.email)
            }
        } else sendReset(input)
    }

    private fun sendReset(email: String) {
        auth.sendPasswordReset(email) { sucesso ->
            Toast.makeText(this, if (sucesso) getString(R.string.reset_email_sent) else getString(R.string.reset_email_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnCreateUser.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) getString(R.string.logging_in) else getString(R.string.login)
    }
}