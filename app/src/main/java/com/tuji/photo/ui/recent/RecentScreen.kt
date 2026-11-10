package com.tuji.photo.ui.recent

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tuji.photo.data.local.EditProjectEntity
import com.tuji.photo.ui.components.MainTab
import com.tuji.photo.ui.components.MainTabBar
import com.tuji.photo.ui.icons.PhotoIcon
import com.tuji.photo.ui.icons.PhotoIconName
import com.tuji.photo.ui.theme.LocalAppPalette

/**
 * 最近编辑（pill 标签栏「编辑」项）：Room 中的编辑项目列表，点击继续编辑。
 */
@Composable
fun RecentScreen(
    onOpenEditor: (Long, String) -> Unit,
    onOpenTemplates: () -> Unit = {},
    onOpenMine: () -> Unit = {},
    viewModel: RecentViewModel = hiltViewModel(),
) {
    val palette = LocalAppPalette.current
    val projects by viewModel.projects.collectAsState()

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
            Text("编辑", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }

        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有编辑记录", fontSize = 14.sp, color = palette.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("去图库选一张照片开始", fontSize = 12.sp, color = palette.textTertiary)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(projects, key = { it.id }) { project ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                            .clickable { onOpenEditor(project.id, project.imageUri) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = project.imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                project.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textPrimary,
                            )
                            Text(
                                "上次编辑 · ${relativeTime(project.lastEditedAt)}",
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                            )
                        }
                        PhotoIcon(
                            PhotoIconName.CHEVRON,
                            tint = palette.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        MainTabBar(
            active = MainTab.EDIT,
            onTemplates = onOpenTemplates,
            onMine = onOpenMine,
        )
    }
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 60 -> "${minutes.coerceAtLeast(1)} 分钟前"
        minutes < 60 * 24 -> "${minutes / 60} 小时前"
        else -> "${minutes / (60 * 24)} 天前"
    }
}
