package com.jywkhyse.meizumusic.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jywkhyse.meizumusic.core.model.Folder
import com.jywkhyse.meizumusic.domain.GetMusicFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val getMusicFolderUseCase: GetMusicFolderUseCase
) : ViewModel() {

    private val _folders = MutableStateFlow<MutableList<Folder>>(mutableListOf())
    val folders = _folders.asStateFlow()

    init {
        viewModelScope.launch {
            _folders.value = getMusicFolderUseCase().toMutableList()
        }
    }
}