package com.tuji.photo.ui.editor

import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BrandingWatermark
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Image
import com.tuji.photo.ui.theme.LocalAppPalette

/** 根据四参数 + 滤镜预设合成实时预览 ColorMatrix（真实生效，非截图模拟）。 */
fun previewMatrix(
    adj: Adjustments,
    preset: FilterPreset?,
    strength: Float,
): ColorMatrix {
    val k = if (preset == null) 0f else strength / 100f
    val satBase = 1f + adj.saturation / 100f
    val satFactor = if (preset == null) satBase else 1f + (preset.saturationMul - 1f) * k * satBase

    val scale = 1f + adj.contrast / 200f + (preset?.contrastAdd ?: 0f) / 200f * k
    val offset = 128f * (1f - scale)
    val brightOffset = adj.brightness * 2.55f
    val temp = adj.temperature + (preset?.temperatureAdd ?: 0f) * k
    val rOffset = offset + brightOffset + temp * 0.4f
    val gOffset = offset + brightOffset
    val bOffset = offset + brightOffset - temp * 0.4f

    val base = AndroidColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, rOffset,
            0f, scale, 0f, 0f, gOffset,
            0f, 0f, scale, 0f, bOffset,
            0f, 0f, 0f, 1f, 0f,
        )
    )
    val sat = AndroidColorMatrix().apply { setToSaturation(satFactor.coerceIn(0f, 2f)) }
    val out = AndroidColorMatrix()
    out.setConcat(base, sat)
    return ColorMatrix(out.values)
}

@Composable
fun EditorScreen(
    onClose: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val palette = LocalAppPalette.current
    val activeTool by viewModel.activeTool.collectAsState()
    val bitmap by viewModel.bitmap.collectAsState()
    val rotation by viewModel.rotation.collectAsState()

    val adj = Adjustments(
        viewModel.brightness.collectAsState().value,
        viewModel.contrast.collectAsState().value,
        viewModel.saturation.collectAsState().value,
        viewModel.temperature.collectAsState().value,
    )
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val filterStrength by viewModel.filterStrength.collectAsState()
    val matrix = remember(adj, selectedFilter, filterStrength) {
        previewMatrix(adj, viewModel.filterPresets.getOrNull(selectedFilter), filterStrength)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶栏：× / 编辑 / 导出
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Close, contentDescription = "关闭",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onClose() },
                )
                Spacer(Modifier.weight(1f))
                Text("编辑", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent)
                        .clickable { viewModel.saveProject(onClose) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("导出", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.onAccent)
                }
            }

            // 照片画布
            val borderStyle by viewModel.borderStyle.collectAsState()
            val borderWidth by viewModel.borderWidth.collectAsState()
            val cropRatio by viewModel.cropRatio.collectAsState()
            val ratios = listOf(null, 1f, 3f / 4f, 4f / 3f, 16f / 9f)

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let { bmp ->
                    val ratio = ratios[cropRatio.coerceIn(0, ratios.lastIndex)]
                    var imageModifier = Modifier
                        .padding(24.dp)
                        .then(
                            if (ratio != null) Modifier.aspectRatio(ratio) else Modifier
                        )
                    // 边框
                    when (borderStyle) {
                        1 -> imageModifier = imageModifier
                            .border(borderWidth.dp, Color.White)
                            .padding(borderWidth.dp)
                        2 -> imageModifier = imageModifier
                            .border(borderWidth.dp, Color.Black)
                            .padding(borderWidth.dp)
                        3 -> imageModifier = imageModifier
                            .clip(RoundedCornerShape(16.dp))
                        4 -> imageModifier = imageModifier
                            .background(Color.White)
                            .padding(horizontal = borderWidth.dp)
                            .padding(bottom = borderWidth.dp * 5)
                    }
                    Image(
                        bitmap = bmp,
                        contentDescription = "照片",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(matrix),
                        modifier = imageModifier.graphicsLayer {
                            rotationZ = rotation * 90f
                        },
                    )
                }
            }

            // 底部常驻横向工具栏
            EditorToolbar(
                active = activeTool,
                onSelect = { viewModel.toggleTool(it) },
            )
        }

        // 功能面板：从工具栏上方滑出（叠加层）
        activeTool?.let { tool ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp),
            ) {
                EditorPanel(viewModel, tool)
            }
        }
    }
}

@Composable
fun EditorToolbar(
    active: EditorTool?,
    onSelect: (EditorTool) -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(palette.background)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorTool.entries.forEach { tool ->
            val selected = tool == active
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(tool) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val icon = when (tool) {
                    EditorTool.ADJUST -> Icons.Outlined.Tune
                    EditorTool.FILTER -> Icons.Outlined.Contrast
                    EditorTool.WATERMARK -> Icons.Outlined.BrandingWatermark
                    EditorTool.BORDER -> Icons.Outlined.CropSquare
                    EditorTool.CROP -> Icons.Outlined.Crop
                    EditorTool.TEXT -> Icons.Outlined.Title
                }
                Icon(
                    icon,
                    contentDescription = tool.label,
                    tint = if (selected) palette.accent else palette.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    tool.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) palette.accent else palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun EditorPanel(viewModel: EditorViewModel, tool: EditorTool) {
    val palette = LocalAppPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(palette.surface)
            .border(1.dp, palette.hairline, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (tool) {
            EditorTool.ADJUST -> AdjustPanel(viewModel)
            EditorTool.FILTER -> FilterPanel(viewModel)
            EditorTool.WATERMARK -> WatermarkPanel(viewModel)
            EditorTool.BORDER -> BorderPanel(viewModel)
            EditorTool.CROP -> CropPanel(viewModel)
            EditorTool.TEXT -> TextPanel(viewModel)
        }
    }
}
