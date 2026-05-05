package com.mateus.microredesocial.ui

import static androidx.core.app.ActivityCompat.finishAffinity;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mateus.microredesocial.auth.UserAuth
import com.mateus.microredesocial.dao.UserDAO
import com.mateus.microredesocial.databinding.ActivitySignUpBinding
import com.mateus.microredesocial.model.User

// [RF1-2] Registration screen with full name, email, password and password confirmation
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val userAuth = UserAuth.getInstance()
    private val userDAO = UserDAO()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCadastrar.setOnClickListener {
            val nome = binding.edtNome.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val senha = binding.edtSenha.text.toString()
            val confirmacao = binding.edtConfirmacaoSenha.text.toString()

            if (!validateFields(nome, email, senha, confirmacao)) return@setOnClickListener

                    binding.btnCadastrar.isEnabled = false

            // [RF1-3] Firebase Authentication
            userAuth.register(
                    email = email,
                    password = senha,
                    onSuccess = { firebaseUser ->
                            firebaseUser?.let { fbUser ->
                            val user = User(
                            id = fbUser.uid,
                            nome = nome,
                            email = email
                    )
                            // Save user profile in Firestore
                            userDAO.saveUser(
                                    user = user,
                                    onSuccess = {
                                            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
                            },
            onFailure = { e ->
                    binding.btnCadastrar.isEnabled = true
                    Toast.makeText(this, "Erro ao salvar perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
                        )
                    }
                },
            onFailure = { e ->
                    binding.btnCadastrar.isEnabled = true
                    Toast.makeText(this, "Erro ao criar conta: ${e.message}", Toast.LENGTH_LONG).show()
            }
            )
        }

        binding.btnVoltar.setOnClickListener { finish() }
    }

    private fun validateFields(nome: String, email: String, senha: String, confirmacao: String): Boolean {
        if (nome.isEmpty()) {
            binding.edtNome.error = "Informe seu nome completo"
            return false
        }
        if (email.isEmpty()) {
            binding.edtEmail.error = "Informe seu e-mail"
            return false
        }
        if (senha.length < 6) {
            binding.edtSenha.error = "A senha deve ter no mínimo 6 caracteres"
            return false
        }
        if (senha != confirmacao) {
            binding.edtConfirmacaoSenha.error = "As senhas não coincidem"
            return false
        }
        return true
    }
}
