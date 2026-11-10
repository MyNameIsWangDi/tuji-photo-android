package com.tuji.photo.ui.editor

import android.graphics.ColorMatrix as AndroidColorMatrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tuji.photo.ui.export.ExportScreen
import com.tuji.photo.ui.icons.PhotoIcon
import com.tuji.photo.ui.icons.PhotoIconName
import com.tuji.photo.ui.theme.LocalAppPalette

/** 根据参数 + 滤镜预设合成实时预览 ColorMatrix（真实生效）。 */
fun previewMatrix(
    adj: Adjustments,
    preset: FilterPreset?,
    strength: Float,
): ColorMatrix {
    val k = if (preset == null) 0f else strength / 100f
    val satBase = 1f + adj.saturation / 100f
    val satFactor = if (preset == null) satBase else 1f + (preset.saturationMul - 1f) * k * satBase

    // 清晰度：以对比度增益近似（ColorMatrix 无法做邻域锐化）
    val sharpen = 1f + adj.sharpness / 100f
    val scale = (1f + adj.contrast / 200f + (preset?.contrastAdd ?: 0f) / 200f * k) * sharpen
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
    val sat = AndroidColorMatrix().apply { setSaturation(satFactor.coerceIn(0f, 2f)) }
    val out = AndroidColorMatrix()
    out.setConcat(base, sat)

    // 色相偏移：RGB 通道按角度做色彩旋转
    if (adj.hue != 0f) {
        val angle = adj.hue / 100f * 60f          // ±60°
        val rad = Math.toRadians(angle.toDouble())
        val cosA = kotlin.math.cos(rad).toFloat()
        val sinA = kotlin.math.sin(rad).toFloat()
        val m = 1f / 3f
        val oneMinus = 1f - cosA
        val hueMatrix = AndroidColorMatrix(
            floatArrayOf(
                cosA + oneMinus * m, oneMinus * m, oneMinus * m, 0f, 0f,
                m, cosA + oneMinus * m, m, 0f, 0f,
                m, m, cosA + oneMinus * m, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        out.setConcat(hueMatrix, out)
    }
    return ColorMatrix(out.array)
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
    val borderStyle by viewModel.borderStyle.collectAsState()
    val borderWidth by viewModel.borderWidth.collectAsState()
    val cropRatio by viewModel.cropRatio.collectAsState()
    val overlays by viewModel.overlays.collectAsState()
    val selectedOverlayId by viewModel.selectedOverlayId.collectAsState()
    val snapEnabled by viewModel.snapEnabled.collectAsState()
    val snapRangePx by viewModel.snapRangePx.collectAsState()
    val freeMode by viewModel.freeMode.collectAsState()
    val wmStyle by viewModel.watermarkStyle.collectAsState()
    val wmPosition by viewModel.watermarkPosition.collectAsState()
    val wmOpacity by viewModel.watermarkOpacity.collectAsState()
    val cropRect by viewModel.cropRect.collectAsState()
    val cropAspect by viewModel.cropAspectLocked.collectAsState()
    val showExport by viewModel.showExport.collectAsState()

    val adj = Adjustments(
        viewModel.brightness.collectAsState().value,
        viewModel.contrast.collectAsState().value,
        viewModel.saturation.collectAsState().value,
        viewModel.temperature.collectAsState().value,
        viewModel.sharpness.collectAsState().value,
        viewModel.hue.collectAsState().value,
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
                PhotoIcon(
                    PhotoIconName.CLOSE, tint = palette.textSecondary,
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
                        .clickable { viewModel.showExport.value = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("导出", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.onAccent)
                }
            }

            // 照片画布 + 叠加元素层
            val ratios = listOf(null, 1f, 3f / 4f, 4f / 3f, 16f / 9f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let { bmp ->
                    val ratio = ratios[cropRatio.coerceIn(0, ratios.lastIndex)]
                    Box(
                        Modifier
                            .padding(24.dp)
                            .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.then(
                                when (borderStyle) {
                                    1 -> Modifier.border(borderWidth.dp, Color.White).padding(borderWidth.dp)
                                    2 -> Modifier.border(borderWidth.dp, Color.Black).padding(borderWidth.dp)
                                    3 -> Modifier.clip(RoundedCornerShape(16.dp))
                                    4 -> Modifier
                                        .background(Color.White)
                                        .padding(horizontal = borderWidth.dp)
                                        .padding(bottom = borderWidth.dp * 5)
                                    else -> Modifier
                                }
                            )
                        ) {
                            Box {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "照片",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.colorMatrix(matrix),
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = rotation * 90f
                                    },
                                )
                                CanvasOverlay(
                                    elements = overlays,
                                    freeMode = freeMode,
                                    snapEnabled = snapEnabled,
                                    snapRangePx = snapRangePx,
                                    selectedId = selectedOverlayId,
                                    onSelect = { viewModel.selectOverlay(it) },
                                    onDrag = { id, dx, dy -> viewModel.moveOverlay(id, dx, dy) },
                                    onDragEnd = { id, anchor -> viewModel.snapOverlay(id, anchor) },
                                    onTapCanvas = { _, _ -> viewModel.freeMode.value = true },
                                    modifier = Modifier.matchParentSize(),
                                )

                                // 水印层（设计稿 04 屏：3×3 位置九宫格）
                                if (activeTool == EditorTool.WATERMARK && wmStyle == 0) {
                                    WatermarkOverlay(
                                        text = "© 途记 · 2026",
                                        anchorIndex = wmPosition,
                                        alpha = wmOpacity / 100f,
                                        modifier = Modifier.matchParentSize(),
                                    )
                                }

                                // 裁剪层（设计稿 06 屏：三分线 + 四角手柄）
                                if (activeTool == EditorTool.CROP) {
                                    CropOverlay(
                                        rect = cropRect,
                                        lockedAspect = cropAspect,
                                        onChange = { viewModel.updateCropRect(it) },
                                        modifier = Modifier.matchParentSize(),
                                    )
                                }
                            }
                        }
                    }
                }

                // 已吸附反馈
                overlays.firstOrNull { it.id == selectedOverlayId }?.let { element ->
                    SnappedChip(
                        anchorIndex = element.snappedAnchor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
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

        // 导出页（设计稿 02 屏）：覆盖在编辑器之上
        if (showExport) {
            ExportScreen(
                viewModel = viewModel,
                onClose = { viewModel.showExport.value = false },
            )
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
                PhotoIcon(
                    iconFor(tool),
                    tint = if (selected) palette.accent else palette.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(8.dp))
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

private fun iconFor(tool: EditorTool): PhotoIconName = when (tool) {
    EditorTool.ADJUST -> PhotoIconName.ADJUST
    EditorTool.FILTER -> PhotoIconName.FILTER
    EditorTool.WATERMARK -> PhotoIconName.WATERMARK
    EditorTool.BORDER -> PhotoIconName.BORDER
    EditorTool.CROP -> PhotoIconName.CROP
    EditorTool.TEXT -> PhotoIconName.TEXT
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
