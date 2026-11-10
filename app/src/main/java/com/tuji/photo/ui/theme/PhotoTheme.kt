package com.tuji.photo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 设计系统（暗房修图台）：
 * 深色中性面板 + 唯一强调色（随主题切换的单一 accent），照片是唯一的亮色来源。
 * 五套主题共用同一套结构 token，只换色板。
 */
enum class AppThemeMode(val label: String, val desc: String) {
    DARKROOM("暗房黑", "默认 · 深色中性"),
    MORNING("晨光白", "明亮 · 适合白天户外"),
    FILM_WARM("胶片暖", "暖调 · 低对比度"),
    GREEN_WILD("青野", "冷绿 · 户外出行主题"),
    JADE_GRAY("墨玉灰", "中性灰 · 柔和过渡"),
}

data class AppPalette(
    val background: Color,
    val surface: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val isDark: Boolean,
)

val LocalAppPalette = staticCompositionLocalOf<AppPalette> {
    error("AppPalette not provided")
}

private val Palettes: Map<AppThemeMode, AppPalette> = mapOf(
    AppThemeMode.DARKROOM to AppPalette(
        background = Color(0xFF070708), surface = Color(0xFF0A0A0A),
        hairline = Color(0xFF1A1A1F),
        textPrimary = Color(0xFFF5F5F7), textSecondary = Color(0xFFA1A1AA),
        textTertiary = Color(0xFF52525B),
        accent = Color(0xFFF5F5F7), onAccent = Color(0xFF070708), isDark = true,
    ),
    AppThemeMode.MORNING to AppPalette(
        background = Color(0xFFFAFAF7), surface = Color(0xFFFFFFFF),
        hairline = Color(0xFFE4E4E0),
        textPrimary = Color(0xFF1A1A1E), textSecondary = Color(0xFF71717A),
        textTertiary = Color(0xFFA1A1AA),
        accent = Color(0xFF0A0A0A), onAccent = Color(0xFFFFFFFF), isDark = false,
    ),
    AppThemeMode.FILM_WARM to AppPalette(
        background = Color(0xFF16120E), surface = Color(0xFF1D1712),
        hairline = Color(0xFF2A221A),
        textPrimary = Color(0xFFF3EDE3), textSecondary = Color(0xFFC7BBA8),
        textTertiary = Color(0xFF6E6455),
        accent = Color(0xFFE8DCC8), onAccent = Color(0xFF16120E), isDark = true,
    ),
    AppThemeMode.GREEN_WILD to AppPalette(
        background = Color(0xFF0C1310), surface = Color(0xFF111B15),
        hairline = Color(0xFF1C2A22),
        textPrimary = Color(0xFFE6F0EA), textSecondary = Color(0xFF9DB8AA),
        textTertiary = Color(0xFF4E6156),
        accent = Color(0xFFCDE8D6), onAccent = Color(0xFF0C1310), isDark = true,
    ),
    AppThemeMode.JADE_GRAY to AppPalette(
        background = Color(0xFF17181B), surface = Color(0xFF1F2023),
        hairline = Color(0xFF2E3034),
        textPrimary = Color(0xFFE4E4E7), textSecondary = Color(0xFFA1A1AA),
        textTertiary = Color(0xFF62626B),
        accent = Color(0xFFD4D4D8), onAccent = Color(0xFF17181B), isDark = true,
    ),
)

/** 全局主题控制器：内存态 + SharedPreferences 持久化。 */
object ThemeController {
    private const val PREFS = "tuji_prefs"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(AppThemeMode.DARKROOM)
        private set

    fun load() {
        val prefs = PhotoApp.instance?.getSharedPreferences(PREFS, 0) ?: return
        runCatching {
            mode = AppThemeMode.valueOf(
                prefs.getString(KEY, AppThemeMode.DARKROOM.name) ?: AppThemeMode.DARKROOM.name
            )
        }
    }

    fun set(target: AppThemeMode) {
        mode = target
        PhotoApp.instance?.getSharedPreferences(PREFS, 0)
            ?.edit()?.putString(KEY, target.name)?.apply()
    }
}

private fun AppPalette.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent,
        background = background, onBackground = textPrimary,
        surface = surface, onSurface = textPrimary,
        surfaceVariant = surface, onSurfaceVariant = textSecondary,
        outline = hairline,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent,
        background = background, onBackground = textPrimary,
        surface = surface, onSurface = textPrimary,
        surfaceVariant = surface, onSurfaceVariant = textSecondary,
        outline = hairline,
    )
}

@Composable
fun PhotoTheme(content: @Composable () -> Unit) {
    val palette = Palettes[ThemeController.mode] ?: Palettes.getValue(AppThemeMode.DARKROOM)
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = Typography(),
            content = content,
        )
    }
}
