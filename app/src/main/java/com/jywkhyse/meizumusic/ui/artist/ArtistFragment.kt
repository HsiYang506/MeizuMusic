package com.jywkhyse.meizumusic.ui.artist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jywkhyse.meizumusic.databinding.FragmentListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class ArtistFragment : Fragment() {

    private var _binding: FragmentListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: ArtistViewModel by viewModels()

    private var adapter: ArtistAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        adapter = ArtistAdapter { position, item ->
            startActivity(Intent(requireContext(), ArtistDetailsActivity::class.java).apply {
                putExtra("id", item.id)
                putExtra("name", item.name)
                putExtra("songCount", item.songCount)
            })
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.artists.collectLatest {
                if (it.isEmpty()) {
                    binding.emptyText.isVisible = true
                    binding.recyclerView.isVisible = false
                    return@collectLatest
                }
                binding.emptyText.isVisible = false
                binding.progressBar.isVisible = false
                binding.recyclerView.isVisible = true
                adapter?.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}