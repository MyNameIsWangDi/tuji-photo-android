package com.tuji.photo.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuji.photo.data.local.EditProjectEntity
import com.tuji.photo.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RecentViewModel @Inject constructor(
    repository: PhotoRepository,
) : ViewModel() {
    val projects: StateFlow<List<EditProjectEntity>> = repository.observeAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
