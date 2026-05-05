package com.mateus.microredesocial.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mateus.microredesocial.R
import com.mateus.microredesocial.auth.UserAuth
import com.mateus.microredesocial.dao.UserDao
import com.mateus.microredesocial.databinding.ActivityProfileBinding
import com.mateus.microredesocial.model.User
import com.mateus.microredesocial.utils.Base64Converter

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = UserAuth()
    private val userDao = UserDao()
    private val converter = Base64Converter()

    private var selectedBitmap: Bitmap? = null
    private var currentPhotoBase64: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        else @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(contentResolver, uri)
        selectedBitmap = bitmap; binding.imgAvatar.setImageBitmap(bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom); insets
        }
        loadUserData(); setupListeners()
    }

    private fun loadUserData() {
        val uid = auth.getCurrentUid() ?: return
        userDao.getUser(uid) { user ->
            user ?: return@getUser
            binding.edtName.setText(user.name)
            binding.edtUsername.setText(user.username)
            binding.edtEmail.setText(user.email)
            currentPhotoBase64 = user.photo
            if (!user.photo.isNullOrEmpty()) binding.imgAvatar.setImageBitmap(converter.stringToBitmap(user.photo))
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.imgAvatar.setOnClickListener { pickImage.launch("image/*") }
        binding.txtAlterarFoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener {
            auth.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun saveProfile() {
        val name = binding.edtName.text.toString().trim()
        val username = binding.edtUsername.text.toString().trim().lowercase()
        if (name.isEmpty()) { binding.tilName.error = "Informe seu nome"; return }
        if (username.isEmpty()) { binding.tilUsername.error = "Informe um nome de usuário"; return }
        binding.tilName.error = null; binding.tilUsername.error = null

        val uid = auth.getCurrentUid() ?: return
        setLoading(true)
        val photoBase64 = selectedBitmap?.let { converter.bitmapToString(it) } ?: currentPhotoBase64
        val updatedUser = User(uid = uid, name = name, username = username, email = binding.edtEmail.text.toString().trim(), photo = photoBase64)

        userDao.save(updatedUser,
            onSuccess = { setLoading(false); Toast.makeText(this, "Perfil atualizado!", Toast.LENGTH_SHORT).show(); setResult(RESULT_OK); finish() },
            onError = { msg -> setLoading(false); Toast.makeText(this, "Erro ao salvar: $msg", Toast.LENGTH_LONG).show() }
        )
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val tilCurrentPassword = dialogView.findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val tilNewPassword = dialogView.findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirmPassword = dialogView.findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val edtCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.edtCurrentPassword)
        val edtNewPassword = dialogView.findViewById<TextInputEditText>(R.id.edtNewPassword)
        val edtConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.edtConfirmPassword)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Alterar senha").setView(dialogView)
            .setPositiveButton("Confirmar", null).setNegativeButton("Cancelar", null).create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                tilCurrentPassword.error = null; tilNewPassword.error = null; tilConfirmPassword.error = null
                val currentPass = edtCurrentPassword.text.toString()
                val newPass = edtNewPassword.text.toString()
                val confirmPass = edtConfirmPassword.text.toString()
                if (currentPass.isEmpty()) { tilCurrentPassword.error = "Informe a senha atual"; return@setOnClickListener }
                if (newPass.length < 6) { tilNewPassword.error = "A nova senha deve ter ao menos 6 caracteres"; return@setOnClickListener }
                if (newPass != confirmPass) { tilConfirmPassword.error = "As senhas não coincidem"; return@setOnClickListener }
                if (newPass == currentPass) { tilNewPassword.error = "A nova senha deve ser diferente da atual"; return@setOnClickListener }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                auth.changePassword(currentPass, newPass) { success, errorMsg ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    if (success) { dialog.dismiss(); Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show() }
                    else tilCurrentPassword.error = errorMsg ?: "Erro ao alterar a senha"
                }
            }
        }
        dialog.show()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.btnLogout.isEnabled = !isLoading
        binding.btnChangePassword.isEnabled = !isLoading
        binding.btnSave.text = if (isLoading) "Salvando..." else "Salvar"
    }
}