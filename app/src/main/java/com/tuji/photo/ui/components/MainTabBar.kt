package com.tuji.photo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.ui.theme.LocalAppPalette

enum class MainTab(val label: String) {
    GALLERY("图库"), EDIT("编辑"), TEMPLATES("模板"), MINE("我的")
}

/** pill 形底部标签栏：图库 / 编辑 / 模板 / 我的。 */
@Composable
fun MainTabBar(
    active: MainTab,
    onGallery: () -> Unit = {},
    onTemplates: () -> Unit = {},
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.hairline.copy(alpha = 0.35f))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == active
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) palette.accent else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable {
                            when (tab) {
                                MainTab.GALLERY -> onGallery()
                                MainTab.TEMPLATES -> onTemplates()
                                else -> {}
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) palette.onAccent else palette.textSecondary,
                    )
                }
            }
        }
    }
}
