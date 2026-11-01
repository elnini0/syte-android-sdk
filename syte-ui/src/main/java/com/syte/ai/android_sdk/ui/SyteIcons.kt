package com.syte.ai.android_sdk.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Line icons the core Material icon set doesn't have, drawn on a 24-unit grid. */
object SyteIcons {

    val Camera = icon("SyteCamera") {
        moveTo(3f, 8f)
        lineTo(7.5f, 8f)
        lineTo(9f, 5f)
        lineTo(15f, 5f)
        lineTo(16.5f, 8f)
        lineTo(21f, 8f)
        lineTo(21f, 19f)
        lineTo(3f, 19f)
        close()
        circle(12f, 13.5f, 3.5f)
    }

    val Photo = icon("SytePhoto") {
        moveTo(3f, 5f)
        lineTo(21f, 5f)
        lineTo(21f, 19f)
        lineTo(3f, 19f)
        close()
        moveTo(3f, 16f)
        lineTo(8.5f, 11f)
        lineTo(13f, 15f)
        lineTo(16f, 12.5f)
        lineTo(21f, 16.5f)
        circle(16f, 8.8f, 1.3f)
    }

    val History = icon("SyteHistory") {
        circle(12f, 12f, 9f)
        moveTo(12f, 7f)
        lineTo(12f, 12f)
        lineTo(15.5f, 14f)
    }

    val Trending = icon("SyteTrending") {
        moveTo(3f, 17f)
        lineTo(9f, 11f)
        lineTo(13f, 15f)
        lineTo(21f, 7f)
        moveTo(15f, 7f)
        lineTo(21f, 7f)
        lineTo(21f, 13f)
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()
}
