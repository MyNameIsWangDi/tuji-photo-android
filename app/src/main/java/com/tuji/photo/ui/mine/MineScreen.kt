package com.tuji.photo.ui.mine

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuji.photo.data.repository.PhotoRepository
import com.tuji.photo.ui.components.MainTab
import com.tuji.photo.ui.components.MainTabBar
import com.tuji.photo.ui.icons.PhotoIcon
import com.tuji.photo.ui.icons.PhotoIconName
import com.tuji.photo.ui.theme.AppThemeMode
import com.tuji.photo.ui.theme.LocalAppPalette
import com.tuji.photo.ui.theme.ThemeController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MineViewModel @Inject constructor(
    repository: PhotoRepository,
) : ViewModel() {
    val projectCount: StateFlow<Int> = repository.observeAllProjects()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
}

/**
 * 我的：外观入口 + 本地项目统计 + 关于。
 */
@Composable
fun MineScreen(
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit,
    viewModel: MineViewModel = hiltViewModel(),
) {
    val palette = LocalAppPalette.current
    val count by viewModel.projectCount.collectAsState()
    val mode = ThemeController.mode

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("我的", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 外观入口
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(swatchOf(mode))
                        .border(1.dp, palette.hairline, RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("外观", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                    Text(
                        "当前主题 · ${mode.label}",
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                    )
                }
                PhotoIcon(PhotoIconName.CHEVRON, tint = palette.textTertiary, modifier = Modifier.size(16.dp))
            }

            // 统计
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(14.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatCell("本地项目", count.toString())
                StatCell("模板", "6")
                StatCell("主题", AppThemeMode.entries.size.toString())
            }

            // 模板入口
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                    .clickable { onOpenTemplates() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("模板中心", fontSize = 14.sp, color = palette.textPrimary)
                Spacer(Modifier.weight(1f))
                PhotoIcon(PhotoIconName.CHEVRON, tint = palette.textTertiary, modifier = Modifier.size(16.dp))
            }

            Text(
                "途记 · v1.0.0",
                fontSize = 11.sp,
                color = palette.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))
        MainTabBar(active = MainTab.MINE)
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    val palette = LocalAppPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        Text(label, fontSize = 11.sp, color = palette.textSecondary)
    }
}

private fun swatchOf(mode: AppThemeMode) = when (mode) {
    AppThemeMode.DARKROOM -> androidx.compose.ui.graphics.Color(0xFF070708)
    AppThemeMode.MORNING -> androidx.compose.ui.graphics.Color(0xFFFAFAF7)
    AppThemeMode.FILM_WARM -> androidx.compose.ui.graphics.Color(0xFF16120E)
    AppThemeMode.GREEN_WILD -> androidx.compose.ui.graphics.Color(0xFF0C1310)
    AppThemeMode.JADE_GRAY -> androidx.compose.ui.graphics.Color(0xFF17181B)
}
