package com.tuji.photo.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuji.photo.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: PhotoRepository,
) : ViewModel() {

    /** 模板封面用图：取相册中最近的一张照片。 */
    val coverUri: StateFlow<String?> = MutableStateFlow<String?>(null).also { flow ->
        viewModelScope.launch {
            flow.value = repository.loadRecentPhotos(limit = 1).firstOrNull()?.uri
        }
    }
}
