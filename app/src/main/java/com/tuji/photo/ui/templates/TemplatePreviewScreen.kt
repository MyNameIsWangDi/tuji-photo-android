package com.tuji.photo.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.data.model.PhotoTemplate
import com.tuji.photo.ui.editor.Adjustments
import com.tuji.photo.ui.editor.previewMatrix
import com.tuji.photo.ui.icons.PhotoIcon
import com.tuji.photo.ui.icons.PhotoIconName
import com.tuji.photo.ui.theme.LocalAppPalette

/**
 * 模板预览（设计稿 04 屏）：预览图 + 模板包含项 chips + 使用模板按钮。
 */
@Composable
fun TemplatePreviewScreen(
    template: PhotoTemplate,
    onBack: () -> Unit,
    onUse: () -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val palette = LocalAppPalette.current
    val coverUri by viewModel.coverUri.collectAsState()
    val templateMatrix = remember(template.id) {
        previewMatrix(
            Adjustments(
                template.brightness, template.contrast,
                template.saturation, template.temperature,
            ),
            null,
            100f,
        )
    }

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
                PhotoIconName.BACK, tint = palette.textPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
            )
            Spacer(Modifier.size(12.dp))
            Text(template.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
        }

        // 预览
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(template.swatch).copy(alpha = 0.28f))
                .border(1.dp, palette.hairline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // 真实照片 + 模板调色效果预览
            coverUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = template.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(templateMatrix),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                template.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary,
            )
            // 水印位置标记
            Box(
                Modifier
                    .align(when (template.watermarkPosition) {
                        0, 1, 2 -> Alignment.TopEnd
                        3, 4, 5 -> Alignment.CenterEnd
                        else -> Alignment.BottomEnd
                    })
                    .padding(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    "© 途记 · 2026",
                    fontSize = 10.sp,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 模板包含项
        Text(
            "模板包含",
            fontSize = 13.sp,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            template.components.take(2).forEach { component ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(component, fontSize = 12.sp, color = palette.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            template.components.drop(2).forEach { component ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(component, fontSize = 12.sp, color = palette.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 使用模板
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.accent)
                .clickable { onUse() },
            contentAlignment = Alignment.Center,
        ) {
            Text("使用模板", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.onAccent)
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "长按预览可对比原图",
            fontSize = 11.sp,
            color = palette.textTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}
