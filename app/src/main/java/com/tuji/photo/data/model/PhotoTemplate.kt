package com.tuji.photo.data.model

/** 一个模板：包含若干项编辑组合。 */
data class PhotoTemplate(
    val id: String,
    val name: String,
    val tag: String,
    val swatch: Long,
    /** 模板包含项（设计稿 04 屏的 chips）。 */
    val components: List<String>,
    /** 套用模板后的调色参数。 */
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
    /** 套用模板后的边框样式。 */
    val borderStyle: Int = 1,
    /** 套用模板后的水印位置（3×3 索引）。 */
    val watermarkPosition: Int = 8,
)

/**
 * 待套用的模板：在模板预览页点「使用模板」后暂存，
 * 用户下一次新建编辑时自动套用（用完即清）。
 */
object PendingTemplate {
    var template: PhotoTemplate? = null

    fun take(): PhotoTemplate? = template.also { template = null }
}

object TemplateCatalog {    val all = listOf(
        PhotoTemplate(
            id = "dusk_cinema",
            name = "暮色电影感",
            tag = "滤镜 · 暖阳",
            swatch = 0xFFC8A879,
            components = listOf("滤镜 · 暖阳", "饱和度 +20", "水印 · 右下", "边框 · 白边"),
            brightness = 6f, contrast = 8f, saturation = 20f, temperature = 12f,
            borderStyle = 1, watermarkPosition = 8,
        ),
        PhotoTemplate(
            id = "city_night",
            name = "城市夜行",
            tag = "滤镜 · 清冷",
            swatch = 0xFF6B7A8F,
            components = listOf("滤镜 · 清冷", "对比度 +12", "亮度 -6", "边框 · 黑边"),
            brightness = -6f, contrast = 12f, saturation = -10f, temperature = -18f,
            borderStyle = 2, watermarkPosition = 0,
        ),
        PhotoTemplate(
            id = "beach_sunset",
            name = "海边日落",
            tag = "滤镜 · 蜜桃",
            swatch = 0xFFE89C7C,
            components = listOf("滤镜 · 蜜桃", "色温 +16", "饱和度 +12", "水印 · 左下"),
            brightness = 4f, contrast = 4f, saturation = 12f, temperature = 16f,
            borderStyle = 0, watermarkPosition = 6,
        ),
        PhotoTemplate(
            id = "morning_note",
            name = "晨间手记",
            tag = "水印 · 右下",
            swatch = 0xFFB8B09A,
            components = listOf("滤镜 · 原图", "亮度 +8", "水印 · 右下", "边框 · 圆角"),
            brightness = 8f, contrast = 0f, saturation = -6f, temperature = 6f,
            borderStyle = 3, watermarkPosition = 8,
        ),
        PhotoTemplate(
            id = "green_holiday",
            name = "青绿假日",
            tag = "滤镜 · 青柠",
            swatch = 0xFF8FAF8E,
            components = listOf("滤镜 · 青柠", "饱和度 +16", "色温 -8", "边框 · 白边"),
            brightness = 2f, contrast = 6f, saturation = 16f, temperature = -8f,
            borderStyle = 1, watermarkPosition = 2,
        ),
        PhotoTemplate(
            id = "neon_night",
            name = "夜色霓虹",
            tag = "滤镜 · 浓郁",
            swatch = 0xFF9C6B8F,
            components = listOf("滤镜 · 浓郁", "对比度 +16", "饱和度 +28", "水印 · 中下"),
            brightness = -4f, contrast = 16f, saturation = 28f, temperature = 4f,
            borderStyle = 0, watermarkPosition = 7,
        ),
    )

    fun byId(id: String): PhotoTemplate? = all.firstOrNull { it.id == id }
}
