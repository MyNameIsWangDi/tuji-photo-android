package com.tuji.photo.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.tuji.photo.ui.theme.LocalAppPalette
import kotlin.math.max
import kotlin.math.min

/** 裁剪框（归一化坐标）。 */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
)

/**
 * 裁剪叠加层：四周压暗 + 三分线 + 四角手柄，手柄可拖动调整裁剪范围。
 * 比例锁定时按目标宽高比约束。
 */
@Composable
fun CropOverlay(
    rect: CropRect,
    lockedAspect: Float?,
    onChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (size.width == 0 || size.height == 0) return@Canvas
            val left = rect.left * size.width
            val top = rect.top * size.height
            val right = rect.right * size.width
            val bottom = rect.bottom * size.height

            // 压暗裁剪区外
            drawRect(
                Color.Black.copy(alpha = 0.45f),
                topLeft = Offset.Zero,
                size = Size(size.width.toFloat(), top),
            )
            drawRect(
                Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(0f, bottom),
                size = Size(size.width.toFloat(), size.height - bottom),
            )
            drawRect(
                Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(0f, top),
                size = Size(left, bottom - top),
            )
            drawRect(
                Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(right, top),
                size = Size(size.width - right, bottom - top),
            )

            // 三分线
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            for (i in 1..2) {
                val x = left + (right - left) * i / 3f
                drawLine(
                    Color.White.copy(alpha = 0.55f),
                    Offset(x, top), Offset(x, bottom),
                    strokeWidth = 1f, pathEffect = dash,
                )
                val y = top + (bottom - top) * i / 3f
                drawLine(
                    Color.White.copy(alpha = 0.55f),
                    Offset(left, y), Offset(right, y),
                    strokeWidth = 1f, pathEffect = dash,
                )
            }

            // 裁剪边框
            drawRect(
                Color.White,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 1.5f),
            )

            // 四角手柄
            listOf(
                Offset(left, top), Offset(right, top),
                Offset(left, bottom), Offset(right, bottom),
            ).forEach { p ->
                drawCircle(palette.accent, radius = 6f, center = p)
                drawCircle(
                    Color.Black.copy(alpha = 0.5f), radius = 2.4f, center = p,
                )
            }
        }

        // 手柄拖动热区
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(rect, lockedAspect) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val dx = drag.x / w
                        val dy = drag.y / h
                        // 以四角之一为拖动点：取离拖动位置最近的角
                        val corners = listOf(
                            rect.left to rect.top, rect.right to rect.top,
                            rect.left to rect.bottom, rect.right to rect.bottom,
                        )
                        val px = (change.position.x / w)
                        val py = (change.position.y / h)
                        var bestIndex = 0
                        var bestDistance = Float.MAX_VALUE
                        corners.forEachIndexed { index, (cx, cy) ->
                            val d = ((cx - px) * (cx - px) + (cy - py) * (cy - py))
                            if (d < bestDistance) {
                                bestDistance = d
                                bestIndex = index
                            }
                        }
                        onChange(
                            applyCornerDrag(rect, bestIndex, dx, dy, lockedAspect, w, h)
                        )
                    }
                }
        )
    }
}

private fun applyCornerDrag(
    rect: CropRect,
    cornerIndex: Int,
    dx: Float,
    dy: Float,
    lockedAspect: Float?,
    canvasW: Float,
    canvasH: Float,
): CropRect {
    var left = rect.left
    var top = rect.top
    var right = rect.right
    var bottom = rect.bottom

    when (cornerIndex) {
        0 -> { left += dx; top += dy }
        1 -> { right += dx; top += dy }
        2 -> { left += dx; bottom += dy }
        3 -> { right += dx; bottom += dy }
    }

    left = left.coerceIn(0f, right - 0.08f)
    top = top.coerceIn(0f, bottom - 0.08f)
    right = right.coerceIn(left + 0.08f, 1f)
    bottom = bottom.coerceIn(top + 0.08f, 1f)

    // 比例锁定：以拖动的角为锚点反推另一边
    if (lockedAspect != null) {
        val anchorRight = cornerIndex == 1 || cornerIndex == 3
        val anchorBottom = cornerIndex >= 2
        val w = (right - left) * canvasW
        val h = (bottom - top) * canvasH
        val targetW = max(h * lockedAspect, 16f)
        val targetH = max(targetW / lockedAspect, 16f)
        val newW = (targetW / canvasW).coerceAtMost(1f - left)
        val newH = (targetH / canvasH).coerceAtMost(1f - top)
        if (anchorRight) right = min(1f, left + newW) else left = max(0f, right - newW)
        if (anchorBottom) bottom = min(1f, top + newH) else top = max(0f, bottom - newH)
    }
    return CropRect(left, top, right, bottom)
}

/**
 * 水印叠加层：按 3×3 位置九宫格把水印文字画在画布上。
 */
@Composable
fun WatermarkOverlay(
    text: String?,
    anchorIndex: Int,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (text.isNullOrBlank() || alpha <= 0f) return
    val palette = LocalAppPalette.current
    val (ax, ay) = Anchors.POINTS[anchorIndex.coerceIn(0, 8)]

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = ax * w
            val cy = ay * h
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                this.alpha = (alpha * 255).toInt()
                textSize = 13f * density
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = when (ax) {
                    0f -> android.graphics.Paint.Align.LEFT
                    1f -> android.graphics.Paint.Align.CENTER
                    else -> android.graphics.Paint.Align.RIGHT
                }
                setShadowLayer(6f * density, 0f, 1f * density, 0x66000000)
            }
            val baseline = cy + paint.textSize / 3f
            drawContext.canvas.nativeCanvas.drawText(text, cx, baseline, paint)
        }
    }
}


/**
 * 图片水印叠加层：按 3×3 位置九宫格把水印图片画在画布上。
 */
@Composable
fun ImageWatermarkOverlay(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    anchorIndex: Int,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f) return
    val (ax, ay) = Anchors.POINTS[anchorIndex.coerceIn(0, 8)]

    Box(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val target = minOf(w, h) * 0.22f
            val left = when (ax) {
                0f -> 0f
                1f -> (w - target) / 2f
                else -> w - target
            }
            val top = when (ay) {
                0f -> 0f
                1f -> (h - target) / 2f
                else -> h - target
            }
            drawImage(
                image = bitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(
                    left.toInt(), top.toInt()
                ),
                dstSize = androidx.compose.ui.unit.IntSize(
                    target.toInt(), target.toInt()
                ),
                alpha = alpha,
            )
        }
    }
}
