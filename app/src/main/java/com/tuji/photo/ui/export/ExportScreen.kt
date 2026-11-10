package com.tuji.photo.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.ui.editor.Adjustments
import com.tuji.photo.ui.editor.Anchors
import com.tuji.photo.ui.editor.EditorViewModel
import com.tuji.photo.ui.editor.OverlayKind
import com.tuji.photo.ui.editor.previewMatrix
import com.tuji.photo.ui.icons.PhotoIcon
import com.tuji.photo.ui.icons.PhotoIconName
import com.tuji.photo.ui.theme.LocalAppPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 导出页（设计稿 02 屏）：成品预览 + 信息卡 + 保存到相册 / 分享。
 */
@Composable
fun ExportScreen(
    viewModel: EditorViewModel,
    onClose: () -> Unit,
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val scope = CoroutineScope(Dispatchers.Main)
    val bitmap by viewModel.bitmap.collectAsState()
    val overlays by viewModel.overlays.collectAsState()
    val borderStyle by viewModel.borderStyle.collectAsState()
    val borderWidth by viewModel.borderWidth.collectAsState()
    val wmPosition by viewModel.watermarkPosition.collectAsState()
    val wmOpacity by viewModel.watermarkOpacity.collectAsState()
    val wmStyle by viewModel.watermarkStyle.collectAsState()

    val adj = Adjustments(
        viewModel.brightness.collectAsState().value,
        viewModel.contrast.collectAsState().value,
        viewModel.saturation.collectAsState().value,
        viewModel.temperature.collectAsState().value,
    )
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val strength by viewModel.filterStrength.collectAsState()
    val matrix = previewMatrix(adj, viewModel.filterPresets.getOrNull(selectedFilter), strength)

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
                PhotoIconName.CLOSE, tint = palette.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() },
            )
            Spacer(Modifier.weight(1f))
            Text("导出", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(24.dp))
        }

        // 成品预览
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.hairline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            bitmap?.let { bmp ->
                Box(
                    Modifier
                        .then(
                            when (borderStyle) {
                                1 -> Modifier.border(borderWidth.dp, Color.White).padding(borderWidth.dp)
                                2 -> Modifier.border(borderWidth.dp, Color.Black).padding(borderWidth.dp)
                                4 -> Modifier
                                    .background(Color.White)
                                    .padding(horizontal = borderWidth.dp)
                                    .padding(bottom = borderWidth.dp * 5)
                                else -> Modifier
                            }
                        )
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp,
                        contentDescription = "预览",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 信息卡
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InfoRow("尺寸", "2048 × 2048")
            InfoRow("格式", "JPG")
            InfoRow("质量", "90%")
            InfoRow("边框", if (borderStyle == 1) "白边" else "无")
            InfoRow("水印", if (wmStyle == 0) "右下 · ${wmOpacity.toInt()}%" else "关闭")
            InfoRow("叠加元素", "${overlays.size} 个")
        }

        Spacer(Modifier.height(24.dp))

        // 操作按钮
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accent)
                    .clickable {
                        val source = bitmap.value?.asAndroidBitmap() ?: return@clickable
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                saveToGallery(
                                    context = context,
                                    source = source,
                                    matrix = matrix,
                                    overlays = overlays,
                                    borderStyle = borderStyle,
                                    borderWidth = borderWidth,
                                    watermark = if (wmStyle == 0) "© 途记 · 2026" else null,
                                    watermarkAnchor = wmPosition,
                                    watermarkAlpha = wmOpacity / 100f,
                                )
                            }
                            android.widget.Toast.makeText(
                                context,
                                if (result != null) "已保存到相册" else "保存失败",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("保存到相册", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.onAccent)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.hairline, RoundedCornerShape(12.dp))
                        .clickable {
                            val source = bitmap.value?.asAndroidBitmap() ?: return@clickable
                            scope.launch {
                                val uri = withContext(Dispatchers.IO) {
                                    saveToGallery(
                                        context = context,
                                        source = source,
                                        matrix = matrix,
                                        overlays = overlays,
                                        borderStyle = borderStyle,
                                        borderWidth = borderWidth,
                                        watermark = if (wmStyle == 0) "© 途记 · 2026" else null,
                                        watermarkAnchor = wmPosition,
                                        watermarkAlpha = wmOpacity / 100f,
                                    )
                                }
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "分享照片")
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("分享", fontSize = 14.sp, color = palette.textPrimary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalAppPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = palette.textSecondary)
        Text(value, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = palette.textPrimary)
    }
}

/**
 * 合成并保存到相册：调色矩阵 → 裁剪 → 边框 → 叠加元素 → 水印。
 */
fun saveToGallery(
    context: Context,
    source: Bitmap,
    matrix: androidx.compose.ui.graphics.ColorMatrix,
    overlays: List<com.tuji.photo.ui.editor.OverlayElement>,
    borderStyle: Int,
    borderWidth: Float,
    watermark: String?,
    watermarkAnchor: Int,
    watermarkAlpha: Float,
): Uri? {
    val composed = composeBitmap(
        source = source,
        matrix = matrix,
        overlays = overlays,
        borderStyle = borderStyle,
        borderWidth = borderWidth,
        watermark = watermark,
        watermarkAnchor = watermarkAnchor,
        watermarkAlpha = watermarkAlpha,
    ) ?: return null

    return runCatching {
        val name = "Tuji_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Tuji")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null
            context.contentResolver.openOutputStream(uri)?.use {
                composed.compress(Bitmap.CompressFormat.JPEG, 92, it)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Tuji"
            )
            dir.mkdirs()
            val file = File(dir, name)
            FileOutputStream(file).use { composed.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            Uri.fromFile(file)
        }
    }.getOrNull()
}

/** 在 Canvas 上完成全部合成。 */
fun composeBitmap(
    source: Bitmap,
    matrix: androidx.compose.ui.graphics.ColorMatrix,
    overlays: List<com.tuji.photo.ui.editor.OverlayElement>,
    borderStyle: Int,
    borderWidth: Float,
    watermark: String?,
    watermarkAnchor: Int,
    watermarkAlpha: Float,
): Bitmap? = runCatching {
    val borderPx = (borderWidth * source.width / 400f).toInt().coerceAtLeast(0)
    val outW = source.width + borderPx * 2
    val outH = source.height + borderPx * 2 + if (borderStyle == 4) borderPx * 4 else 0

    val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(output)
    canvas.drawColor(if (borderStyle == 1 || borderStyle == 4) android.graphics.Color.WHITE else android.graphics.Color.TRANSPARENT)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix.values)
    }
    canvas.drawBitmap(source, borderPx.toFloat(), borderPx.toFloat(), paint)

    // 叠加元素
    overlays.forEach { element ->
        val x = borderPx + element.x * source.width
        val y = borderPx + element.y * source.height
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = element.fontSizeSp * source.width / 320f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        when (element.kind) {
            OverlayKind.TEXT, OverlayKind.WATERMARK ->
                canvas.drawText(element.content, x, y, textPaint)
            OverlayKind.STICKER -> {
                val radius = source.width / 22f
                canvas.drawCircle(x, y, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(90, 0, 0, 0)
                })
                textPaint.textSize = radius * 1.4f
                canvas.drawText(element.content, x, y + radius * 0.4f, textPaint)
            }
        }
    }

    // 水印
    if (!watermark.isNullOrBlank() && watermarkAlpha > 0f) {
        val (ax, ay) = Anchors.POINTS[watermarkAnchor.coerceIn(0, 8)]
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = (watermarkAlpha * 255).toInt()
            textSize = source.width / 26f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = when (ax) {
                0f -> Paint.Align.LEFT
                1f -> Paint.Align.CENTER
                else -> Paint.Align.RIGHT
            }
            setShadowLayer(source.width / 120f, 0f, source.width / 320f, 0x66000000)
        }
        canvas.drawText(
            watermark,
            borderPx + ax * source.width,
            borderPx + ay * source.height,
            wmPaint,
        )
    }
    output
}.getOrNull()
