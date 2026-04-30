package com.mateus.microredesocial.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mateus.microredesocial.auth.UserAuth
import com.mateus.microredesocial.dao.UserDAO
import com.mateus.microredesocial.databinding.ActivityProfileBinding
import com.mateus.microredesocial.utils.Base64Converter

// [RF3-3] Edit profile - name, password and profile photo
class ProfileAtivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val userAuth = UserAuth.getInstance()
    private val userDAO = UserDAO()
    private var selectedBitmap: Bitmap? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            selectedBitmap = bitmap
            binding.imgFotoPerfil.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        val userId = userAuth.getCurrentUser()?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        userDAO.getUser(
            userId = userId,
            onSuccess = { user ->
                binding.progressBar.visibility = View.GONE
                user?.let {
                    binding.edtNomePerfil.setText(it.nome)
                    binding.txtEmailPerfil.text = it.email

                    if (it.fotoString.isNotEmpty()) {
                        val bitmap = Base64Converter.stringToBitmap(it.fotoString)
                        bitmap?.let { bmp -> binding.imgFotoPerfil.setImageBitmap(bmp) }
                    }
                }
            },
            onFailure = { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        // Change profile photo
        binding.btnAlterarFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        // [RF3-3] Save profile changes
        binding.btnSalvarPerfil.setOnClickListener {
            saveProfile()
        }

        binding.btnVoltar.setOnClickListener { finish() }
    }

    private fun saveProfile() {
        val userId = userAuth.getCurrentUser()?.uid ?: return
        val novoNome = binding.edtNomePerfil.text.toString().trim()
        val novaSenha = binding.edtNovaSenha.text.toString()
        val confirmacaoSenha = binding.edtConfirmacaoSenhaPerfil.text.toString()

        if (novoNome.isEmpty()) {
            binding.edtNomePerfil.error = "Informe seu nome"
            return
        }

        if (novaSenha.isNotEmpty()) {
            if (novaSenha.length < 6) {
                binding.edtNovaSenha.error = "Senha deve ter no mínimo 6 caracteres"
                return
            }
            if (novaSenha != confirmacaoSenha) {
                binding.edtConfirmacaoSenhaPerfil.error = "As senhas não coincidem"
                return
            }
        }

        binding.btnSalvarPerfil.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val updates = mutableMapOf<String, Any>("nome" to novoNome)

        selectedBitmap?.let {
            val fotoString = Base64Converter.bitmapToString(it)
            updates["fotoString"] = fotoString
        }

        userDAO.updateUser(
            userId = userId,
            updates = updates,
            onSuccess = {
                if (novaSenha.isNotEmpty()) {
                    updatePassword(novaSenha)
                } else {
                    onProfileSaved()
                }
            },
            onFailure = { e ->
                binding.btnSalvarPerfil.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updatePassword(newPassword: String) {
        userAuth.updatePassword(
            newPassword = newPassword,
            onSuccess = { onProfileSaved() },
            onFailure = { e ->
                binding.btnSalvarPerfil.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Perfil salvo, mas erro ao alterar senha: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun onProfileSaved() {
        binding.btnSalvarPerfil.isEnabled = true
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        binding.edtNovaSenha.setText("")
        binding.edtConfirmacaoSenhaPerfil.setText("")
        selectedBitmap = null
    }
}
