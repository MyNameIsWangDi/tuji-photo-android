package com.tuji.photo.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuji.photo.ui.theme.LocalAppPalette
import kotlin.math.hypot
import kotlin.math.roundToInt

/** 叠加元素类型：文字 / 贴纸 / 水印。 */
enum class OverlayKind { TEXT, STICKER, WATERMARK }

/**
 * 画布上的一个可拖动元素。x / y 为相对画布的归一化坐标（0..1）。
 */
data class OverlayElement(
    val id: Long,
    val kind: OverlayKind,
    val content: String,
    val x: Float,
    val y: Float,
    /** 吸附命中的锚点索引，-1 表示未吸附。 */
    val snappedAnchor: Int = -1,
    val fontSizeSp: Float = 14f,
    val colorArgb: Long = 0xFFFFFFFF,
)

/** 九宫锚点：四角 + 四边中点 + 中心。 */
object Anchors {
    val NAMES = listOf("左上", "中上", "右上", "左中", "中心", "右中", "左下", "中下", "右下")
    val POINTS = listOf(
        0f to 0f, 0.5f to 0f, 1f to 0f,
        0f to 0.5f, 0.5f to 0.5f, 1f to 0.5f,
        0f to 1f, 0.5f to 1f, 1f to 1f,
    )

    /** 找到距离最近的锚点；超出阈值返回 -1。 */
    fun nearest(x: Float, y: Float, threshold: Float): Int {
        var best = -1
        var bestDistance = Float.MAX_VALUE
        POINTS.forEachIndexed { index, (ax, ay) ->
            val d = hypot(x - ax, y - ay)
            if (d < bestDistance) {
                bestDistance = d
                best = index
            }
        }
        return if (bestDistance <= threshold) best else -1
    }
}

/**
 * 画布叠加层：承载可拖动的文字 / 贴纸 / 水印，并在「自由摆放」模式下显示
 * 九宫定位点、中心十字与吸附环。
 */
@Composable
fun CanvasOverlay(
    elements: List<OverlayElement>,
    freeMode: Boolean,
    snapEnabled: Boolean,
    snapRangePx: Float,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onDrag: (id: Long, deltaX: Float, deltaY: Float) -> Unit,
    onDragEnd: (id: Long, anchorIndex: Int) -> Unit,
    onTapCanvas: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (freeMode && canvasSize.width > 0 && canvasSize.height > 0) {
                        onTapCanvas(
                            offset.x / canvasSize.width,
                            offset.y / canvasSize.height,
                        )
                    }
                }
            },
    ) {
        // 自由摆放：锚点 + 中心十字 + 吸附环
        if (freeMode && canvasSize.width > 0) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                // 中心十字（虚线）
                drawLine(
                    palette.textTertiary.copy(alpha = 0.55f),
                    Offset(w / 2, 0f), Offset(w / 2, h),
                    strokeWidth = 1f, pathEffect = dash,
                )
                drawLine(
                    palette.textTertiary.copy(alpha = 0.55f),
                    Offset(0f, h / 2), Offset(w, h / 2),
                    strokeWidth = 1f, pathEffect = dash,
                )
                // 九宫锚点
                Anchors.POINTS.forEach { (ax, ay) ->
                    val c = Offset(ax * w, ay * h)
                    drawCircle(
                        palette.textPrimary.copy(alpha = 0.45f),
                        radius = 4f, center = c,
                    )
                }
                // 吸附环（半径 = 吸附范围）
                if (snapEnabled) {
                    drawCircle(
                        palette.accent.copy(alpha = 0.35f),
                        radius = snapRangePx,
                        center = Offset(w / 2, h / 2),
                        style = Stroke(width = 1f),
                    )
                }
            }
        }

        // 可拖动元素
        elements.forEach { element ->
            val xPx = element.x * canvasSize.width
            val yPx = element.y * canvasSize.height
            val selected = element.id == selectedId
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (xPx - 48f * density.density).roundToInt(),
                            (yPx - 20f * density.density).roundToInt(),
                        )
                    }
                    .size(width = 96.dp, height = 40.dp)
                    .pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onSelect(element.id) },
                            onDragEnd = {
                                val minSide = minOf(canvasSize.width, canvasSize.height).coerceAtLeast(1)
                                val threshold = snapRangePx / minSide
                                val current = elements.firstOrNull { it.id == element.id }
                                val anchor =
                                    if (snapEnabled && current != null)
                                        Anchors.nearest(current.x, current.y, threshold) else -1
                                onDragEnd(element.id, anchor)
                            },
                            onDragCancel = { onDragEnd(element.id, -1) },
                        ) { change, drag ->
                            change.consume()
                            onDrag(
                                element.id,
                                drag.x / canvasSize.width.coerceAtLeast(1),
                                drag.y / canvasSize.height.coerceAtLeast(1),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when (element.kind) {
                    OverlayKind.TEXT, OverlayKind.WATERMARK -> {
                        Text(
                            element.content,
                            fontSize = element.fontSizeSp.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(element.colorArgb),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }

                    OverlayKind.STICKER -> {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                element.content,
                                fontSize = 18.sp,
                                color = Color(element.colorArgb),
                            )
                        }
                    }
                }

                // 选中框 + 四角定位点
                if (selected) {
                    Canvas(Modifier.fillMaxSize()) {
                        val rect = androidx.compose.ui.geometry.Rect(
                            Offset(size.width / 2 - 48f * density.density, size.height / 2 - 20f * density.density),
                            Offset(size.width / 2 + 48f * density.density, size.height / 2 + 20f * density.density),
                        )
                        drawRect(
                            palette.accent.copy(alpha = 0.9f),
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = Stroke(width = 1.2f),
                        )
                        listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight)
                            .forEach { p ->
                                drawCircle(palette.accent, radius = 3.5f, center = p)
                            }
                    }
                }
            }
        }
    }
}

/** 已吸附反馈 chip（设计稿 08 屏）。 */
@Composable
fun SnappedChip(anchorIndex: Int, modifier: Modifier = Modifier) {
    val palette = LocalAppPalette.current
    if (anchorIndex < 0) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accent)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            "已吸附 · ${Anchors.NAMES[anchorIndex]}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = palette.onAccent,
        )
    }
}
