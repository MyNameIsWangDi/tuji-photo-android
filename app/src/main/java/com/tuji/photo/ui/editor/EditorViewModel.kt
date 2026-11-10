package com.tuji.photo.ui.editor

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuji.photo.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 底部常驻工具栏的 6 个工具。 */
enum class EditorTool(val label: String) {
    ADJUST("调色"), FILTER("滤镜"), WATERMARK("水印"),
    BORDER("边框"), CROP("裁剪"), TEXT("文字"),
}

/** 调色四参数，范围 -100..100。 */
data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
) {
    fun toJson(): String = "[$brightness,$contrast,$saturation,$temperature]"

    companion object {
        fun fromJson(raw: String): Adjustments = runCatching {
            val nums = raw.removeSurrounding("[", "]")
                .split(",").map { it.trim().toFloat() }
            Adjustments(
                nums.getOrElse(0) { 0f },
                nums.getOrElse(1) { 0f },
                nums.getOrElse(2) { 0f },
                nums.getOrElse(3) { 0f },
            )
        }.getOrDefault(Adjustments())
    }
}

/** 滤镜预设：在手动调色之上叠加固定偏移。 */
data class FilterPreset(
    val name: String,
    val category: String,
    val saturationMul: Float,
    val temperatureAdd: Float,
    val contrastAdd: Float,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val repository: PhotoRepository,
) : ViewModel() {

    val projectId: Long = savedStateHandle.get<Long>("projectId") ?: 0L
    val imageUri: String = savedStateHandle.get<String>("imageUri") ?: ""

    val bitmap = MutableStateFlow<ImageBitmap?>(null)

    val activeTool = MutableStateFlow<EditorTool?>(null)

    // 调色
    val brightness = MutableStateFlow(0f)
    val contrast = MutableStateFlow(0f)
    val saturation = MutableStateFlow(0f)
    val temperature = MutableStateFlow(0f)

    // 滤镜
    val selectedFilter = MutableStateFlow(-1)
    val filterStrength = MutableStateFlow(70f)

    // 水印
    val watermarkStyle = MutableStateFlow(0)      // 0 文字 / 1 图片
    val watermarkPosition = MutableStateFlow(8)   // 3x3 位置索引，默认右下
    val watermarkOpacity = MutableStateFlow(60f)

    // 边框
    val borderStyle = MutableStateFlow(1)         // 0 无 / 1 白边 / 2 黑边 / 3 圆角 / 4 拍立得
    val borderWidth = MutableStateFlow(6f)
    val borderColorIndex = MutableStateFlow(0)

    // 裁剪
    val cropRatio = MutableStateFlow(2)           // 0 自由 / 1 1:1 / 2 3:4 / 3 4:3 / 4 16:9
    val rotation = MutableStateFlow(0)            // 0/1/2/3 × 90°

    // 文字
    val textContent = MutableStateFlow("山湖暮色 · 2026")
    val textFont = MutableStateFlow(0)
    val textColorIndex = MutableStateFlow(0)
    val textSize = MutableStateFlow(16f)

    val filterPresets = listOf(
        FilterPreset("原图", "全部", 1f, 0f, 0f),
        FilterPreset("暖阳", "胶片", 1.1f, 18f, 5f),
        FilterPreset("胶片", "胶片", 0.85f, 12f, -8f),
        FilterPreset("黑白", "黑白", 0f, 0f, 12f),
        FilterPreset("青橙", "电影", 1.25f, -20f, 10f),
        FilterPreset("复古", "胶片", 0.7f, 22f, -5f),
        FilterPreset("清冷", "电影", 0.9f, -26f, 8f),
        FilterPreset("浓郁", "电影", 1.45f, 6f, 16f),
        FilterPreset("青柠", "风景", 1.15f, -14f, 4f),
        FilterPreset("蜜桃", "风景", 1.2f, 24f, 2f),
    )

    init {
        loadBitmap()
    }

    private fun loadBitmap() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { decodeSampled(imageUri) }
            bitmap.value = loaded
        }
    }

    private fun decodeSampled(uriString: String): ImageBitmap? {
        if (uriString.isBlank()) return null
        return runCatching {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap()
            }
        }.getOrNull()
    }

    fun toggleTool(tool: EditorTool) {
        activeTool.value = if (activeTool.value == tool) null else tool
    }

    fun adjustments(): Adjustments = Adjustments(
        brightness.value, contrast.value, saturation.value, temperature.value
    )

    fun saveProject(onSaved: () -> Unit) {
        if (projectId <= 0L) return onSaved()
        viewModelScope.launch {
            repository.saveAdjustments(projectId, adjustments().toJson())
            onSaved()
        }
    }
}
