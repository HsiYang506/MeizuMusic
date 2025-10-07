// 文件: /app/src/main/java/com/jywkhyse/meizumusic/ui/SearchActivity.kt
package com.jywkhyse.meizumusic.ui.search


import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jywkhyse.meizumusic.databinding.ActivitySearchBinding
import com.jywkhyse.meizumusic.player.SharedPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var resultsAdapter: SearchAdapter // 需要创建一个新的 Adapter

    @Inject
    lateinit var sharedViewModel: SharedPlayerViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBar()
        setupRecyclerView()
        setupSearchBar()
        observeViewModel()

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        resultsAdapter = SearchAdapter(supportFragmentManager) { pos, item ->
            Timber.d("setupRecyclerView: ${item.title}")
            sharedViewModel.playSingleSong(item.toMedia3MediaItem())
        }
        binding.recyclerViewResults.apply {
            adapter = resultsAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
        }
    }

    private fun setupStatusBar() {
        val isNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
    }

    private fun setupSearchBar() {
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onQueryChanged(text.toString())
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                resultsAdapter.submitList(binding.searchEditText.text.toString(), results)
                binding.emptyText.text =
                    if (viewModel.isLoading.value || results.isNotEmpty()) "" else "无结果"
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
                if (isLoading) {
                    binding.emptyText.isVisible = false
                } else {
                    binding.emptyText.isVisible = resultsAdapter.itemCount == 0
                }
            }
        }
    }
}