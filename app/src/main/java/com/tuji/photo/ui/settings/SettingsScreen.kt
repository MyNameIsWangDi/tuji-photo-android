package com.tuji.photo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.ui.icons.PhotoIcon
import com.tuji.photo.ui.icons.PhotoIconName
import com.tuji.photo.ui.theme.AppThemeMode
import com.tuji.photo.ui.theme.LocalAppPalette
import com.tuji.photo.ui.theme.ThemeController

/**
 * 外观设置（主题）：5 套主题卡 + 跟随系统 + 按时间自动切换。
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val palette = LocalAppPalette.current
    var autoSwitch by remember { mutableStateOf(false) }
    var followSystem by remember { mutableStateOf(false) }
    val current = ThemeController.mode

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoIcon(
                PhotoIconName.BACK, tint = palette.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
            )
            Spacer(Modifier.size(12.dp))
            Text("外观", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 节标题
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("界面主题", fontSize = 13.sp, color = palette.textSecondary)
                Text("跟随系统", fontSize = 11.sp, color = palette.textTertiary)
            }

            // 主题卡列表
            AppThemeMode.entries.forEach { mode ->
                val selected = mode == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                        .clickable { ThemeController.set(mode) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 色板预览
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(swatchOf(mode))
                            .border(1.dp, palette.hairline, RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            mode.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary,
                        )
                        Text(
                            if (selected) "当前主题 · ${mode.desc}" else mode.desc,
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                        )
                    }
                    // 选中态
                    Box(
                        Modifier.size(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, palette.accent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent),
                                )
                            }
                        } else {
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, palette.textTertiary, CircleShape),
                            )
                        }
                    }
                }
            }

            // 跟随系统
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                    .clickable { followSystem = !followSystem }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("跟随系统", fontSize = 14.sp, color = palette.textPrimary)
                Switch(
                    checked = followSystem,
                    onCheckedChange = { followSystem = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.onAccent,
                        checkedTrackColor = palette.accent,
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.hairline,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }

            // 按时间自动切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                    .clickable { autoSwitch = !autoSwitch }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("按时间自动切换", fontSize = 14.sp, color = palette.textPrimary)
                Switch(
                    checked = autoSwitch,
                    onCheckedChange = { autoSwitch = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.onAccent,
                        checkedTrackColor = palette.accent,
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.hairline,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }

            Text(
                "主题设置将同步到所有设备",
                fontSize = 11.sp,
                color = palette.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun swatchOf(mode: AppThemeMode): Color = when (mode) {
    AppThemeMode.DARKROOM -> Color(0xFF070708)
    AppThemeMode.MORNING -> Color(0xFFFAFAF7)
    AppThemeMode.FILM_WARM -> Color(0xFF16120E)
    AppThemeMode.GREEN_WILD -> Color(0xFF0C1310)
    AppThemeMode.JADE_GRAY -> Color(0xFF17181B)
}
