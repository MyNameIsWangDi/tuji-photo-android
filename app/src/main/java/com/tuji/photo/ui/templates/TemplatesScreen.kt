package com.tuji.photo.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.data.model.TemplateCatalog
import com.tuji.photo.ui.theme.AppThemeMode
import com.tuji.photo.ui.theme.LocalAppPalette
import com.tuji.photo.ui.theme.ThemeController

@Composable
fun TemplatesScreen(
    onBack: () -> Unit,
    onOpenPreview: (String) -> Unit = {},
    onOpenMine: () -> Unit = {},
    onOpenRecent: () -> Unit = {},
) {
    val palette = LocalAppPalette.current
    var category by remember { mutableIntStateOf(0) }

    val templates = TemplateCatalog.all

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
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
                PhotoIconName.BACK,
                tint = palette.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
            )
            Spacer(Modifier.size(12.dp))
            Text("模板", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
        }

        // 分类 chips
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("精选", "调色", "拼图", "水印").forEachIndexed { i, label ->
                val selected = i == category
                Box(
                    Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) palette.accent else palette.surface)
                        .border(1.dp, if (selected) palette.accent else palette.hairline, RoundedCornerShape(16.dp))
                        .clickable { category = i }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        color = if (selected) palette.onAccent else palette.textSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 模板卡片网格
        templates.chunked(2).forEach { rowCards ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowCards.forEach { card ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(14.dp))
                            .clickable { onOpenPreview(card.id) }
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color(card.swatch).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(card.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                        }
                        Column(Modifier.padding(12.dp)) {
                            Text(card.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                            Text(card.tag, fontSize = 11.sp, color = palette.textTertiary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 主题外观：横向选择列表（放在模板功能下方）
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("主题外观", fontSize = 13.sp, color = palette.textSecondary)
            Text("${AppThemeMode.entries.size} 套", fontSize = 11.sp, color = palette.textTertiary)
        }
        ThemeHorizontalList()

        Spacer(Modifier.height(16.dp))
        com.tuji.photo.ui.components.MainTabBar(
            active = com.tuji.photo.ui.components.MainTab.TEMPLATES,
            onGallery = onBack,
            onEdit = onOpenRecent,
            onMine = onOpenMine,
        )
    }
}

@Composable
private fun ThemeHorizontalList() {
    val palette = LocalAppPalette.current
    val current = ThemeController.mode
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppThemeMode.entries.forEach { mode ->
            val selected = mode == current
            Row(
                Modifier
                    .width(104.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .border(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) palette.accent else palette.hairline,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { ThemeController.set(mode) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(swatchFor(mode))
                        .border(1.dp, palette.hairline, RoundedCornerShape(8.dp)),
                )
                Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
            }
        }
    }
}

private fun swatchFor(mode: AppThemeMode): Color = when (mode) {
    AppThemeMode.DARKROOM -> Color(0xFF070708)
    AppThemeMode.MORNING -> Color(0xFFFAFAF7)
    AppThemeMode.FILM_WARM -> Color(0xFF16120E)
    AppThemeMode.GREEN_WILD -> Color(0xFF0C1310)
    AppThemeMode.JADE_GRAY -> Color(0xFF17181B)
}
