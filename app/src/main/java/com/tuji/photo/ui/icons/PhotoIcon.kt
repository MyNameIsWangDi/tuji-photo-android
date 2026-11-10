package com.tuji.photo.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * 设计稿统一图标语言：24×24 网格、1.6px 线性描边、圆头端点，
 * 不使用 Material 风格的多色填充图标，保持「暗房修图台」的克制感。
 */
enum class PhotoIconName {
    ADJUST, FILTER, WATERMARK, BORDER, CROP, TEXT,
    CLOSE, BACK, GEAR, CHEVRON, CHECK, PLUS, SHARE, SAVE,
}

@Composable
fun PhotoIcon(
    name: PhotoIconName,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.6.dp,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val w = size.width
        val h = size.height
        fun x(v: Float) = v / 24f * w
        fun y(v: Float) = v / 24f * h

        when (name) {
            // 调色：双滑杆
            PhotoIconName.ADJUST -> {
                drawLine(tint, Offset(x(3f), y(8f)), Offset(x(21f), y(8f)), stroke, StrokeCap.Round)
                drawCircle(tint, radius = 2.8f / 24f * w, center = Offset(x(8f), y(8f)))
                drawLine(tint, Offset(x(3f), y(16f)), Offset(x(21f), y(16f)), stroke, StrokeCap.Round)
                drawCircle(tint, radius = 2.8f / 24f * w, center = Offset(x(15f), y(16f)))
            }

            // 滤镜：半填充圆
            PhotoIconName.FILTER -> {
                drawCircle(
                    tint, radius = 7.5f / 24f * w, center = Offset(x(12f), y(12f)),
                    style = Stroke(width = stroke),
                )
                val path = Path().apply {
                    moveTo(x(12f), y(4.5f))
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            Offset(x(4.5f), y(4.5f)), Size(15f / 24f * w, 15f / 24f * h)
                        ),
                        startAngleDegrees = -90f, sweepAngleDegrees = 180f, forceMoveTo = true,
                    )
                    lineTo(x(12f), y(19.5f))
                    close()
                }
                drawPath(path, tint)
            }

            // 水印：矩形 + 横线
            PhotoIconName.WATERMARK -> {
                drawRoundRect(
                    tint, Offset(x(3.5f), y(5.5f)), Size(x(17f) - x(3.5f), y(16.5f) - y(5.5f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(1.5f), y(1.5f)),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint, Offset(x(7f), y(12.2f)), Offset(x(17f), y(12.2f)),
                    2f / 24f * w, StrokeCap.Round,
                )
            }

            // 边框：嵌套方框
            PhotoIconName.BORDER -> {
                drawRoundRect(
                    tint, Offset(x(3.5f), y(3.5f)), Size(x(17f) - x(3.5f), y(17f) - y(3.5f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(1.5f), y(1.5f)),
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    tint.copy(alpha = 0.55f), Offset(x(7.5f), y(7.5f)),
                    Size(x(9f) - x(7.5f), y(9f) - x(7.5f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(0.8f), y(0.8f)),
                    style = Stroke(width = stroke * 0.85f),
                )
            }

            // 裁剪：L 形
            PhotoIconName.CROP -> {
                val p = Path().apply {
                    moveTo(x(7f), y(3f)); lineTo(x(7f), y(15.5f))
                    quadraticTo(x(7f), y(17f), x(8.5f), y(17f)); lineTo(x(19.5f), y(17f))
                }
                drawPath(p, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
                val p2 = Path().apply {
                    moveTo(x(3f), y(7f)); lineTo(x(15.5f), y(7f))
                    quadraticTo(x(17f), y(7f), x(17f), y(8.5f)); lineTo(x(17f), y(19.5f))
                }
                drawPath(p2, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }

            // 文字：T
            PhotoIconName.TEXT -> {
                drawLine(tint, Offset(x(4.5f), y(6.5f)), Offset(x(19.5f), y(6.5f)), 2f / 24f * w, StrokeCap.Round)
                drawLine(tint, Offset(x(12f), y(6.5f)), Offset(x(12f), y(17f)), 2f / 24f * w, StrokeCap.Round)
            }

            PhotoIconName.CLOSE -> {
                drawLine(tint, Offset(x(7f), y(7f)), Offset(x(17f), y(17f)), stroke, StrokeCap.Round)
                drawLine(tint, Offset(x(17f), y(7f)), Offset(x(7f), y(17f)), stroke, StrokeCap.Round)
            }

            PhotoIconName.BACK -> {
                val p = Path().apply {
                    moveTo(x(14.5f), y(6f)); lineTo(x(8f), y(12f)); lineTo(x(14.5f), y(18f))
                }
                drawPath(p, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }

            PhotoIconName.CHEVRON -> {
                val p = Path().apply {
                    moveTo(x(10f), y(6f)); lineTo(x(15f), y(12f)); lineTo(x(10f), y(18f))
                }
                drawPath(p, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }

            PhotoIconName.CHECK -> {
                val p = Path().apply {
                    moveTo(x(6f), y(12.5f)); lineTo(x(10.5f), y(17f)); lineTo(x(18f), y(7f))
                }
                drawPath(p, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }

            PhotoIconName.PLUS -> {
                drawLine(tint, Offset(x(12f), y(6f)), Offset(x(12f), y(18f)), stroke, StrokeCap.Round)
                drawLine(tint, Offset(x(6f), y(12f)), Offset(x(18f), y(12f)), stroke, StrokeCap.Round)
            }

            // 齿轮：圆 + 齿
            PhotoIconName.GEAR -> {
                drawCircle(tint, radius = 3.2f / 24f * w, center = Offset(x(12f), y(12f)), style = Stroke(width = stroke))
                val r1 = 6.5f / 24f * w
                val r2 = 8.5f / 24f * w
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    drawLine(
                        tint,
                        Offset(x(12f) + (r1 * kotlin.math.cos(a)).toFloat(), y(12f) + (r1 * kotlin.math.sin(a)).toFloat()),
                        Offset(x(12f) + (r2 * kotlin.math.cos(a)).toFloat(), y(12f) + (r2 * kotlin.math.sin(a)).toFloat()),
                        stroke, StrokeCap.Round,
                    )
                }
            }

            // 分享：节点连线
            PhotoIconName.SHARE -> {
                drawCircle(tint, radius = 2.2f / 24f * w, center = Offset(x(6.5f), y(12f)), style = Stroke(width = stroke))
                drawCircle(tint, radius = 2.2f / 24f * w, center = Offset(x(17.5f), y(6f)), style = Stroke(width = stroke))
                drawCircle(tint, radius = 2.2f / 24f * w, center = Offset(x(17.5f), y(18f)), style = Stroke(width = stroke))
                drawLine(tint, Offset(x(8.4f), y(10.8f)), Offset(x(15.6f), y(7.2f)), stroke)
                drawLine(tint, Offset(x(8.4f), y(13.2f)), Offset(x(15.6f), y(16.8f)), stroke)
            }

            // 保存：向下箭头入托盘
            PhotoIconName.SAVE -> {
                val p = Path().apply {
                    moveTo(x(12f), y(4f)); lineTo(x(12f), y(15f))
                    moveTo(x(7.5f), y(10.5f)); lineTo(x(12f), y(15f)); lineTo(x(16.5f), y(10.5f))
                }
                drawPath(p, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
                val p2 = Path().apply {
                    moveTo(x(4.5f), y(17.5f)); lineTo(x(4.5f), y(20f)); lineTo(x(19.5f), y(20f))
                    lineTo(x(19.5f), y(17.5f))
                }
                drawPath(p2, tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
    }
}
