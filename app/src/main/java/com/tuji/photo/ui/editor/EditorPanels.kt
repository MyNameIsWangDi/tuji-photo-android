package com.tuji.photo.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.ui.theme.LocalAppPalette

/** 面板顶部的二级功能 chips 行（设计稿中的核心交互）。 */
@Composable
fun SecondaryChipRow(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) palette.accent else palette.background)
                    .border(
                        1.dp,
                        if (isSelected) palette.accent else palette.hairline,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) palette.onAccent else palette.textSecondary,
                )
            }
        }
    }
}

/** 参数滑杆：标签 + 等宽数字值（JetBrains Mono 风格 → monospace）。 */
@Composable
fun ParamSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
) {
    val palette = LocalAppPalette.current
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, fontSize = 13.sp, color = palette.textPrimary)
            Box(Modifier.weight(1f))
            Text(
                valueText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = palette.textSecondary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.hairline,
            ),
        )
    }
}

@Composable
internal fun AdjustPanel(viewModel: EditorViewModel) {
    val palette = LocalAppPalette.current
    var section by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val brightness by viewModel.brightness.collectAsState()
    val contrast by viewModel.contrast.collectAsState()
    val saturation by viewModel.saturation.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val sharpness by viewModel.sharpness.collectAsState()
    val hue by viewModel.hue.collectAsState()

    SecondaryChipRow(
        options = listOf("光效", "色彩", "细节", "曲线", "HSL"),
        selected = section,
        onSelect = { section = it },
    )

    // 二级分组决定当前显示的参数（设计稿 02 屏）
    when (section) {
        0 -> {
            ParamSlider("亮度", brightness, { viewModel.brightness.value = it },
                valueText = formatSigned(brightness))
            ParamSlider("对比度", contrast, { viewModel.contrast.value = it },
                valueText = formatSigned(contrast))
        }
        1 -> {
            ParamSlider("饱和度", saturation, { viewModel.saturation.value = it },
                valueText = formatSigned(saturation))
            ParamSlider("色温", temperature, { viewModel.temperature.value = it },
                valueText = formatSigned(temperature))
        }
        2 -> {
            ParamSlider("清晰度", sharpness, { viewModel.sharpness.value = it },
                valueText = formatSigned(sharpness))
            ParamSlider("对比度", contrast, { viewModel.contrast.value = it },
                valueText = formatSigned(contrast))
        }
        3 -> {
            ParamSlider("亮度", brightness, { viewModel.brightness.value = it },
                valueText = formatSigned(brightness))
            ParamSlider("清晰度", sharpness, { viewModel.sharpness.value = it },
                valueText = formatSigned(sharpness))
        }
        else -> {
            ParamSlider("色相", hue, { viewModel.hue.value = it },
                valueText = formatSigned(hue))
            ParamSlider("饱和度", saturation, { viewModel.saturation.value = it },
                valueText = formatSigned(saturation))
        }
    }
    Text("长按画布可对比原图", fontSize = 12.sp, color = palette.textTertiary)
}

@Composable
internal fun FilterPanel(viewModel: EditorViewModel) {
    val palette = LocalAppPalette.current
    var category by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val strength by viewModel.filterStrength.collectAsState()
    val currentBitmap by viewModel.bitmap.collectAsState()

    val categories = listOf("全部", "胶片", "黑白", "电影", "风景")
    SecondaryChipRow(categories, category) { category = it }

    val visible = viewModel.filterPresets.withIndex().filter { (_, p) ->
        category == 0 || p.category == categories[category] || p.name == "原图"
    }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(visible.size) { indexInVisible ->
            val (originalIndex, preset) = visible[indexInVisible]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.background)
                        .border(
                            if (originalIndex == selectedFilter) 2.dp else 1.dp,
                            if (originalIndex == selectedFilter) palette.accent else palette.hairline,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { viewModel.selectedFilter.value = originalIndex },
                    contentAlignment = Alignment.Center,
                ) {
                    // 真实照片缩略图 + 该预设的实时效果
                    val thumb = currentBitmap
                    if (thumb != null) {
                        androidx.compose.foundation.Image(
                            bitmap = thumb,
                            contentDescription = preset.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                previewMatrix(Adjustments(), preset, 100f)
                            ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(preset.name.take(1), color = palette.textSecondary, fontSize = 18.sp)
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                Text(
                    preset.name,
                    fontSize = 11.sp,
                    color = if (originalIndex == selectedFilter) palette.textPrimary else palette.textSecondary,
                )
            }
        }
    }
    ParamSlider(
        "强度", strength, { viewModel.filterStrength.value = it },
        valueText = "${strength.toInt()}",
        valueRange = 0f..100f,
    )
}

@Composable
internal fun WatermarkPanel(viewModel: EditorViewModel) {
    val palette = LocalAppPalette.current
    var preset by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val style by viewModel.watermarkStyle.collectAsState()
    val position by viewModel.watermarkPosition.collectAsState()
    val opacity by viewModel.watermarkOpacity.collectAsState()

    SecondaryChipRow(listOf("预设", "自定义", "时间戳", "定位"), preset) { preset = it }

    // 图片水印：调起系统选图
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.setWatermarkImage(it.toString()) } }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("文字水印", "图片水印").forEachIndexed { i, label ->
            val selected = i == style
            Box(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) palette.background else palette.background)
                    .border(1.dp, if (selected) palette.accent else palette.hairline, RoundedCornerShape(16.dp))
                    .clickable {
                        viewModel.watermarkStyle.value = i
                        if (i == 1) {
                            imagePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, color = if (selected) palette.textPrimary else palette.textSecondary)
            }
        }
    }

    if (style == 1) {
        val wmImage by viewModel.watermarkImage.collectAsState()
        Box(
            Modifier
                .height(56.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.background)
                .border(1.dp, palette.hairline, RoundedCornerShape(8.dp))
                .clickable {
                    imagePicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (wmImage != null) {
                androidx.compose.foundation.Image(
                    bitmap = wmImage!!,
                    contentDescription = "水印图片",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    "点击更换",
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(start = 76.dp),
                )
            } else {
                Text("选择水印图片", fontSize = 12.sp, color = palette.textSecondary)
            }
        }
    } else {
        Text("© 途记 · 2026", fontSize = 13.sp, color = palette.textPrimary)
        Text("编辑", fontSize = 12.sp, color = palette.textTertiary)
    }

    // 3×3 位置九宫格
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    val selected = index == position
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) palette.accent else palette.background)
                            .border(1.dp, palette.hairline, RoundedCornerShape(6.dp))
                            .clickable { viewModel.watermarkPosition.value = index },
                    )
                }
            }
        }
    }
    ParamSlider("不透明度", opacity, { viewModel.watermarkOpacity.value = it },
        valueText = "${opacity.toInt()}%", valueRange = 0f..100f)
}

@Composable
internal fun BorderPanel(viewModel: EditorViewModel) {
    val palette = LocalAppPalette.current
    var category by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val style by viewModel.borderStyle.collectAsState()
    val width by viewModel.borderWidth.collectAsState()
    val colorIndex by viewModel.borderColorIndex.collectAsState()
    val borderBitmap by viewModel.bitmap.collectAsState()

    SecondaryChipRow(listOf("基础", "胶片", "拍立得", "装饰"), category) { category = it }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("无边框", "白边", "黑边", "圆角", "拍立得").forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (i == 1 || i == 4) Color.White else palette.background)
                        .border(
                            if (i == style) 2.dp else 1.dp,
                            if (i == style) palette.accent else palette.hairline,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { viewModel.borderStyle.value = i }
                        .padding(if (i == 1 || i == 2) 4.dp else 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val thumb = borderBitmap
                    if (thumb != null) {
                        androidx.compose.foundation.Image(
                            bitmap = thumb,
                            contentDescription = label,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(if (i == 3) 20.dp else 4.dp)),
                        )
                    } else {
                        Text(label.take(1), color = palette.textSecondary, fontSize = 14.sp)
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                Text(label, fontSize = 11.sp, color = palette.textSecondary)
            }
        }
    }
    ParamSlider("边框宽度", width, { viewModel.borderWidth.value = it },
        valueText = "${width.toInt()}px", valueRange = 0f..32f)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("边框颜色", fontSize = 13.sp, color = palette.textPrimary)
        listOf(Color.White, Color.Black, Color(0xFFD4D4D8), Color(0xFFC8A879)).forEachIndexed { i, color ->
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        if (i == colorIndex) 2.dp else 1.dp,
                        if (i == colorIndex) palette.accent else palette.hairline,
                        CircleShape,
                    )
                    .clickable { viewModel.borderColorIndex.value = i },
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("+ 文字", "+ 贴纸").forEach { label ->
            Box(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
                    .clickable {
                        if (label == "+ 文字") {
                            viewModel.addTextOverlay(viewModel.textContent.value)
                        } else {
                            viewModel.addStickerOverlay(viewModel.stickerGlyphs.first())
                        }
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, color = palette.textPrimary)
            }
        }
    }
    // 贴纸图标行（设计稿 08 屏）
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        viewModel.stickerGlyphs.forEach { glyph ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.background)
                    .border(1.dp, palette.hairline, RoundedCornerShape(10.dp))
                    .clickable { viewModel.addStickerOverlay(glyph) },
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, fontSize = 16.sp, color = palette.textPrimary)
            }
        }
    }
    Text("文字与贴纸可拖入边框区域，靠近定位点自动吸附", fontSize = 11.sp, color = palette.textTertiary)
}

@Composable
internal fun CropPanel(viewModel: EditorViewModel) {
    val palette = LocalAppPalette.current
    var mode by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val ratio by viewModel.cropRatio.collectAsState()
    val rotation by viewModel.rotation.collectAsState()

    SecondaryChipRow(listOf("比例", "旋转", "矫正", "透视"), mode) { mode = it }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("自由", "1:1", "3:4", "4:3", "16:9").forEachIndexed { i, label ->
            val selected = i == ratio
            Box(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) palette.accent else palette.background)
                    .border(1.dp, if (selected) palette.accent else palette.hairline, RoundedCornerShape(16.dp))
                    .clickable { viewModel.setCropRatio(i) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (selected) palette.onAccent else palette.textSecondary,
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("左转", "右转", "镜像", "重置").forEachIndexed { i, label ->
            Box(
                Modifier
                    .height(56.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.background)
                    .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                    .clickable {
                        when (i) {
                            0 -> viewModel.rotation.value = (rotation + 3) % 4
                            1 -> viewModel.rotation.value = (rotation + 1) % 4
                            3 -> viewModel.rotation.value = 0
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, color = palette.textPrimary)
            }
        }
    }
    Text("拖动四角手柄可自由调整裁剪范围", fontSize = 11.sp, color = palette.textTertiary)
}

@Composable
internal fun TextPanel(viewModel: EditorViewModel) {
    val palette = LocalAppPalette.current
    var mode by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    val font by viewModel.textFont.collectAsState()
    val colorIndex by viewModel.textColorIndex.collectAsState()
    val size by viewModel.textSize.collectAsState()
    val content by viewModel.textContent.collectAsState()
    val snapOn by viewModel.snapEnabled.collectAsState()
    val snapRange by viewModel.snapRangePx.collectAsState()

    SecondaryChipRow(listOf("文本", "标签", "贴字", "艺术字"), mode) { mode = it }
    Text(
        content.ifBlank { "输入文字…" },
        fontSize = 14.sp, color = palette.textPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.background)
            .border(1.dp, palette.hairline, RoundedCornerShape(8.dp))
            .clickable { viewModel.addTextOverlay(content, size) }
            .padding(12.dp),
    )    // 贴纸图标行（设计稿 08 屏）
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        viewModel.stickerGlyphs.forEach { glyph ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.background)
                    .border(1.dp, palette.hairline, RoundedCornerShape(10.dp))
                    .clickable { viewModel.addStickerOverlay(glyph) },
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, fontSize = 16.sp, color = palette.textPrimary)
            }
        }
    }
    // 定位吸附开关 + 吸附范围
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("定位吸附", fontSize = 13.sp, color = palette.textPrimary)
        Box(
            Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (snapOn) palette.accent else palette.hairline)
                .clickable { viewModel.snapEnabled.value = !snapOn },
            contentAlignment = if (snapOn) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (snapOn) palette.onAccent else palette.textSecondary),
            )
        }
    }
    ParamSlider(
        "吸附范围", snapRange,
        { viewModel.snapRangePx.value = it },
        valueText = "${snapRange.toInt()}px",
        valueRange = 8f..64f,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("标准", "手写", "衬线").forEachIndexed { i, label ->
            val selected = i == font
            Box(
                Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) palette.accent else palette.background)
                    .border(1.dp, if (selected) palette.accent else palette.hairline, RoundedCornerShape(16.dp))
                    .clickable {
                        viewModel.textFont.value = i
                        viewModel.applyTextStyleToSelection()
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 12.sp, color = if (selected) palette.onAccent else palette.textSecondary)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("颜色", fontSize = 13.sp, color = palette.textPrimary)
        listOf(Color.White, Color.Black, Color(0xFFE8B04B), Color(0xFFE87C9C)).forEachIndexed { i, color ->
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        if (i == colorIndex) 2.dp else 1.dp,
                        if (i == colorIndex) palette.accent else palette.hairline,
                        CircleShape,
                    )
                    .clickable {
                        viewModel.textColorIndex.value = i
                        viewModel.applyTextStyleToSelection()
                    },
            )
        }
    }
    ParamSlider("大小", size, {
        viewModel.textSize.value = it
        viewModel.applyTextStyleToSelection()
    },
        valueText = "${size.toInt()}", valueRange = 8f..48f)
    Text("拖动可调整文字位置，靠近定位点自动吸附", fontSize = 11.sp, color = palette.textTertiary)
}

private fun formatSigned(value: Float): String =
    if (value >= 0f) "+${value.toInt()}" else value.toInt().toString()
