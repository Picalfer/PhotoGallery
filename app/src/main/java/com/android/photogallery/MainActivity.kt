package com.android.photogallery

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.photogallery.api.RetrofitClient
import com.android.photogallery.databinding.ActivityMainBinding
import com.android.photogallery.models.ImageResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: ImageAdapter
    private val images = mutableListOf<ImageResult>()
    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }

        binding.imagesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = ImageAdapter(images)
        binding.imagesRecyclerView.adapter = adapter

        // Добавляем обработчик скролла для пагинации
        binding.imagesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount

                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && currentPage < totalPages) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadMoreData()
                    }
                }
            }
        })

        // Обработка кнопки поиска
        binding.searchButton.setOnClickListener {
            val query = binding.searchEditText.text.toString()
            if (query.isNotEmpty()) {
                currentPage = 1 // Сбрасываем на первую страницу при новом поиске
                searchImages(query)
            } else {
                Toast.makeText(this, "Please enter search query", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchImages(query: String) {
        if (isLoading) return

        isLoading = true
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.searchImages(
                    query = query,
                    page = currentPage,
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        apiResponse?.let {
                            totalPages = it.page_count

                            if (currentPage == 1) {
                                images.clear()
                            }

                            it.results?.let { results ->
                                images.addAll(results)
                                adapter.notifyDataSetChanged()
                            }

                            Toast.makeText(
                                this@MainActivity,
                                currentPage.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${response.code()} - ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("API_ERROR", e.toString())
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    showLoading(false)
                }
            }
        }
    }

    private fun loadMoreData() {
        if (isLoading) return

        currentPage++
        val query = binding.searchEditText.text.toString()
        if (query.isNotEmpty()) {
            searchImages(query)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility =
            if (show) View.VISIBLE else View.GONE
    }
}