package com.mateus.microredesocial.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mateus.microredesocial.adapter.PostAdapter
import com.mateus.microredesocial.auth.UserAuth
import com.mateus.microredesocial.dao.PostDAO
import com.mateus.microredesocial.dao.UserDAO
import com.mateus.microredesocial.databinding.ActivityHomeBinding
import com.mateus.microredesocial.model.Post
import com.mateus.microredesocial.utils.Base64Converter
import com.mateus.microredesocial.utils.LocationHelper

// [RF3-1] Feed loaded from Firestore (5 at a time with pagination)
// [RF3-2] Search posts by city
// [RF2-1] Create post with image, text and city
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val userAuth = UserAuth.getInstance()
    private val postDAO = PostDAO()
    private val userDAO = UserDAO()

    private val posts = mutableListOf<Post>()
    private lateinit var adapter: PostAdapter
    private var isLoading = false
    private var isSearchMode = false

    private var selectedBitmap: Bitmap? = null
    private var currentCity: String = ""

    // Image picker from gallery
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            selectedBitmap = bitmap
            showCreatePostDialog()
        }
    }

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchLocationAndOpenPostDialog()
        } else {
            showCreatePostDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadFeed()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(posts)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Infinite scroll for pagination [RF3-1]
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isSearchMode) return
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && postDAO.canLoadMore() && lastVisible >= totalItemCount - 2) {
                    loadNextPage()
                }
            }
        })
    }

    private fun setupListeners() {
        // Logout
        binding.btnSair.setOnClickListener {
            userAuth.logout()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        // Go to profile
        binding.btnPerfil.setOnClickListener {
            startActivity(Intent(this, ProfileAtivity::class.java))
        }

        // Create post - open gallery first
        binding.fabNovoPost.setOnClickListener {
            openGallery()
        }

        // Search by city [RF3-2]
        binding.btnBuscar.setOnClickListener {
            val query = binding.edtBusca.text.toString().trim()
            if (query.isEmpty()) {
                resetFeed()
            } else {
                searchByCity(query)
            }
        }

        // Clear search
        binding.btnLimparBusca.setOnClickListener {
            binding.edtBusca.setText("")
            resetFeed()
        }
    }

    // [RF3-1] Load feed with pagination
    private fun loadFeed() {
        isLoading = true
        binding.progressBar.visibility = View.VISIBLE
        postDAO.resetPagination()
        postDAO.loadNextPage(
            onSuccess = { newPosts ->
                isLoading = false
                binding.progressBar.visibility = View.GONE
                adapter.addPosts(newPosts)
                binding.txtEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            },
            onFailure = { e ->
                isLoading = false
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar feed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadNextPage() {
        isLoading = true
        binding.progressBarBottom.visibility = View.VISIBLE
        postDAO.loadNextPage(
            onSuccess = { newPosts ->
                isLoading = false
                binding.progressBarBottom.visibility = View.GONE
                if (newPosts.isNotEmpty()) {
                    adapter.addPosts(newPosts)
                }
            },
            onFailure = { e ->
                isLoading = false
                binding.progressBarBottom.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar mais posts", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun resetFeed() {
        isSearchMode = false
        posts.clear()
        adapter.notifyDataSetChanged()
        loadFeed()
    }

    // [RF3-2] Search posts by city
    private fun searchByCity(cidade: String) {
        isSearchMode = true
        binding.progressBar.visibility = View.VISIBLE
        postDAO.searchByCity(
            cidade = cidade,
            onSuccess = { results ->
                binding.progressBar.visibility = View.GONE
                adapter.setPosts(results)
                binding.txtEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            },
            onFailure = { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro na busca: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // [RF2-1] Open gallery to select image
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // [RF4-1] Fetch location before opening post dialog
    private fun fetchLocationAndOpenPostDialog() {
        Toast.makeText(this, "Obtendo localização...", Toast.LENGTH_SHORT).show()
        LocationHelper.getCurrentCity(this) { city ->
            runOnUiThread {
                currentCity = city ?: ""
                showCreatePostDialog()
            }
        }
    }

    private fun openGalleryWithLocationCheck() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // [RF4-1] Check permission and get location
    private fun checkLocationPermissionAndFetch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                fetchLocationAndOpenPostDialog()
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // [RF2-1] Dialog to create post
    private fun showCreatePostDialog() {
        val bitmap = selectedBitmap ?: return

        val dialogView = layoutInflater.inflate(
            com.mateus.microredesocial.R.layout.dialog_create_post, null
        )
        val edtDescricao = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.mateus.microredesocial.R.id.edtDescricaoPost
        )
        val imgPreview = dialogView.findViewById<android.widget.ImageView>(
            com.mateus.microredesocial.R.id.imgPreviewPost
        )
        val txtCidadeDialog = dialogView.findViewById<android.widget.TextView>(
            com.mateus.microredesocial.R.id.txtCidadeDialog
        )
        val btnObterLocalizacao = dialogView.findViewById<android.widget.Button>(
            com.mateus.microredesocial.R.id.btnObterLocalizacao
        )

        imgPreview.setImageBitmap(bitmap)
        if (currentCity.isNotEmpty()) {
            txtCidadeDialog.text = "📍 $currentCity"
        }

        btnObterLocalizacao.setOnClickListener {
            checkLocationPermissionAndFetch()
        }

        AlertDialog.Builder(this)
            .setTitle("Nova Postagem")
            .setView(dialogView)
            .setPositiveButton("Publicar") { _, _ ->
                val descricao = edtDescricao?.text.toString().trim()
                if (descricao.isEmpty()) {
                    Toast.makeText(this, "Escreva uma descrição", Toast.LENGTH_SHORT).show()
                } else {
                    createPost(bitmap, descricao)
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                selectedBitmap = null
                currentCity = ""
            }
            .show()
    }

    // [RF2-2] Send post to Firestore
    private fun createPost(bitmap: Bitmap, descricao: String) {
        val user = userAuth.getCurrentUser() ?: return
        binding.progressBar.visibility = View.VISIBLE

        userDAO.getUser(
            userId = user.uid,
            onSuccess = { userData ->
                val imageString = Base64Converter.bitmapToString(bitmap)
                val post = Post(
                    descricao = descricao,
                    imageString = imageString,
                    cidade = currentCity,
                    autorId = user.uid,
                    autorNome = userData?.nome ?: user.email ?: "Usuário",
                    autorFoto = userData?.fotoString ?: "",
                    timestamp = System.currentTimeMillis()
                )

                postDAO.savePost(
                    post = post,
                    onSuccess = {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Post publicado com sucesso!", Toast.LENGTH_SHORT).show()
                        selectedBitmap = null
                        currentCity = ""
                        resetFeed()
                    },
                    onFailure = { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Erro ao publicar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao obter dados do usuário", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // Reload feed when returning from profile
    }
}
