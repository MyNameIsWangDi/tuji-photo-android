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

/** 调色参数，范围 -100..100。 */
data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
    /** 清晰度（对比度近似 + 轻锐化观感）。 */
    val sharpness: Float = 0f,
    /** 色相偏移 -100..100。 */
    val hue: Float = 0f,
) {
    fun toJson(): String =
        "[$brightness,$contrast,$saturation,$temperature,$sharpness,$hue]"

    companion object {
        fun fromJson(raw: String): Adjustments = runCatching {
            val nums = raw.removeSurrounding("[", "]")
                .split(",").map { it.trim().toFloat() }
            Adjustments(
                nums.getOrElse(0) { 0f },
                nums.getOrElse(1) { 0f },
                nums.getOrElse(2) { 0f },
                nums.getOrElse(3) { 0f },
                nums.getOrElse(4) { 0f },
                nums.getOrElse(5) { 0f },
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
    val showExport = MutableStateFlow(false)

    // 调色
    val brightness = MutableStateFlow(0f)
    val contrast = MutableStateFlow(0f)
    val saturation = MutableStateFlow(0f)
    val temperature = MutableStateFlow(0f)
    val sharpness = MutableStateFlow(0f)
    val hue = MutableStateFlow(0f)

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
    val cropRect = MutableStateFlow(CropRect())
    val cropAspectLocked = MutableStateFlow<Float?>(null)

    /** 比例 chips → 锁定裁剪宽高比。 */
    fun setCropRatio(index: Int) {
        cropRatio.value = index
        cropAspectLocked.value = when (index) {
            1 -> 1f
            2 -> 3f / 4f
            3 -> 4f / 3f
            4 -> 16f / 9f
            else -> null
        }
    }

    fun updateCropRect(rect: CropRect) {
        cropRect.value = rect
    }

    // 文字
    val textContent = MutableStateFlow("山湖暮色 · 2026")
    val textFont = MutableStateFlow(0)
    val textColorIndex = MutableStateFlow(0)
    val textSize = MutableStateFlow(16f)

    /* ---------- 画布叠加元素（文字 / 贴纸） ---------- */
    val overlays = MutableStateFlow<List<OverlayElement>>(emptyList())
    val selectedOverlayId = MutableStateFlow<Long?>(null)
    val snapEnabled = MutableStateFlow(true)
    val snapRangePx = MutableStateFlow(24f)
    val freeMode = MutableStateFlow(false)

    /** 设计稿 08 屏的贴纸图标集。 */
    val stickerGlyphs = listOf("★", "♥", "⌖", "日", "▣")

    /** 文字颜色（与文字面板色点一致）。 */
    val textColors = listOf(0xFFFFFFFFL, 0xFF101014L, 0xFFE8B04BL, 0xFFE87C9CL)

    private var nextOverlayId = 1L

    /** 边框面板「+ 文字」：把文字拖入画布并开启自由摆放。 */
    fun addTextOverlay(content: String, sizeSp: Float = 16f) {
        val element = OverlayElement(
            id = nextOverlayId++,
            kind = OverlayKind.TEXT,
            content = content.ifBlank { textContent.value },
            x = 0.5f, y = 0.5f,
            snappedAnchor = 4,
            fontSizeSp = sizeSp,
            colorArgb = textColors[textColorIndex.value.coerceIn(0, textColors.lastIndex)],
            fontStyle = textFont.value,
        )
        overlays.value = overlays.value + element
        selectedOverlayId.value = element.id
        freeMode.value = true
    }

    /** 把字体 / 颜色 / 大小应用到选中的文字元素。 */
    fun applyTextStyleToSelection() {
        val id = selectedOverlayId.value ?: return
        overlays.value = overlays.value.map { element ->
            if (element.id != id) element
            else element.copy(
                fontSizeSp = textSize.value,
                colorArgb = textColors[textColorIndex.value.coerceIn(0, textColors.lastIndex)],
                fontStyle = textFont.value,
            )
        }
    }

    /** 边框面板「+ 贴纸」：贴纸进入画布并自动吸附中心。 */
    fun addStickerOverlay(glyph: String) {
        val element = OverlayElement(
            id = nextOverlayId++,
            kind = OverlayKind.STICKER,
            content = glyph,
            x = 0.5f, y = 0.5f,
            snappedAnchor = 4,
        )
        overlays.value = overlays.value + element
        selectedOverlayId.value = element.id
        freeMode.value = true
    }

    fun moveOverlay(id: Long, deltaX: Float, deltaY: Float) {
        overlays.value = overlays.value.map { element ->
            if (element.id != id) element
            else element.copy(
                x = (element.x + deltaX).coerceIn(0f, 1f),
                y = (element.y + deltaY).coerceIn(0f, 1f),
                snappedAnchor = -1,
            )
        }
    }

    /** 拖动结束：命中锚点则吸附。 */
    fun snapOverlay(id: Long, anchorIndex: Int) {
        if (anchorIndex < 0) {
            overlays.value = overlays.value.map {
                if (it.id == id) it.copy(snappedAnchor = -1) else it
            }
            return
        }
        val (ax, ay) = Anchors.POINTS[anchorIndex]
        overlays.value = overlays.value.map { element ->
            if (element.id != id) element
            else element.copy(x = ax, y = ay, snappedAnchor = anchorIndex)
        }
    }

    fun selectOverlay(id: Long?) {
        selectedOverlayId.value = id
    }

    fun removeOverlay(id: Long) {
        overlays.value = overlays.value.filterNot { it.id == id }
        if (selectedOverlayId.value == id) selectedOverlayId.value = null
    }

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
        loadProject()
    }

    /** 打开已有项目时，恢复上次的参数与模板设置。 */
    private fun loadProject() {
        if (projectId <= 0L) return
        viewModelScope.launch {
            val project = repository.getProject(projectId) ?: return@launch
            val saved = Adjustments.fromJson(project.adjustmentsJson)
            if (saved != Adjustments()) {
                brightness.value = saved.brightness
                contrast.value = saved.contrast
                saturation.value = saved.saturation
                temperature.value = saved.temperature
            }
            borderStyle.value = project.borderStyle
            watermarkPosition.value = project.watermarkPosition
        }
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
        brightness.value, contrast.value, saturation.value, temperature.value,
        sharpness.value, hue.value,
    )

    fun saveProject(onSaved: () -> Unit) {
        if (projectId <= 0L) return onSaved()
        viewModelScope.launch {
            repository.saveAdjustments(projectId, adjustments().toJson())
            onSaved()
        }
    }
}
