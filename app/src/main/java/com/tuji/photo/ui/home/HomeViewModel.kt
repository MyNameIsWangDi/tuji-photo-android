package com.tuji.photo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuji.photo.data.repository.GalleryPhoto
import com.tuji.photo.data.repository.PhotoRepository
import com.tuji.photo.data.local.EditProjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PhotoRepository,
) : ViewModel() {

    val photos: StateFlow<List<GalleryPhoto>> = kotlinx.coroutines.flow.flow {
        emit(repository.loadRecentPhotos())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val latestProject: StateFlow<EditProjectEntity?> =
        repository.observeLatestProject()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun createProject(imageUri: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createProject(imageUri))
        }
    }
}
