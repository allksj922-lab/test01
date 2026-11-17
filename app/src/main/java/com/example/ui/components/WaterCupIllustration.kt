package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.WaterCupEmptyBg
import com.example.ui.theme.WaterCupLiquid
import com.example.ui.theme.WaterCupLiquidHighlight
import com.example.ui.theme.WaterCupStroke

@Composable
fun WaterCupIllustration(
  waterCups: Int,
  goalCups: Int,
  modifier: Modifier = Modifier,
) {
  val targetFraction = (waterCups.coerceIn(0, goalCups).toFloat() / goalCups.coerceAtLeast(1))
  val animatedFill by
    animateFloatAsState(
      targetValue = targetFraction,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
      label = "waterFill",
    )

  val cupDescription = stringResource(id = R.string.cd_water_cup, waterCups)

  Canvas(
    modifier =
      modifier.size(width = 92.dp, height = 110.dp).semantics {
        contentDescription = cupDescription
      }
  ) {
    val strokeWidth = 4.5.dp.toPx()
    val inset = strokeWidth / 2f
    val topLeftX = size.width * 0.12f
    val topRightX = size.width * 0.88f
    val bottomLeftX = size.width * 0.22f
    val bottomRightX = size.width * 0.78f
    val topY = inset + size.height * 0.06f
    val bottomY = size.height - inset - size.height * 0.04f
    val cornerCurve = size.width * 0.08f

    val cupPath =
      Path().apply {
        moveTo(topLeftX, topY)
        lineTo(bottomLeftX, bottomY - cornerCurve)
        quadraticTo(bottomLeftX, bottomY, bottomLeftX + cornerCurve, bottomY)
        lineTo(bottomRightX - cornerCurve, bottomY)
        quadraticTo(bottomRightX, bottomY, bottomRightX, bottomY - cornerCurve)
        lineTo(topRightX, topY)
        close()
      }

    // Empty glass background
    drawPath(path = cupPath, color = WaterCupEmptyBg)

    // Animated water fill clipped inside the glass
    if (animatedFill > 0.005f) {
      clipPath(path = cupPath) {
        val cupHeight = bottomY - topY
        val waterTopY = bottomY - (cupHeight * animatedFill * 0.92f)
        val waterHeight = bottomY - waterTopY + 4.dp.toPx()

        drawRect(
          color = WaterCupLiquid,
          topLeft = Offset(0f, waterTopY),
          size = Size(size.width, waterHeight),
        )

        // Brighter water surface meniscus band
        val waveBandHeight = 8.dp.toPx()
        drawRoundRect(
          color = WaterCupLiquidHighlight,
          topLeft = Offset(0f, waterTopY - waveBandHeight * 0.35f),
          size = Size(size.width, waveBandHeight),
          cornerRadius = CornerRadius(waveBandHeight, waveBandHeight),
        )

        // Soft highlight bubbles inside water
        if (animatedFill > 0.2f) {
          drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = 3.5.dp.toPx(),
            center = Offset(size.width * 0.38f, (waterTopY + bottomY) * 0.52f),
          )
        }
        if (animatedFill > 0.5f) {
          drawCircle(
            color = Color.White.copy(alpha = 0.38f),
            radius = 2.5.dp.toPx(),
            center = Offset(size.width * 0.62f, (waterTopY + bottomY) * 0.45f),
          )
        }
      }
    }

    // Subtle measurement guide ticks on the right inner side
    val cupInnerHeight = bottomY - topY
    listOf(0.25f, 0.5f, 0.75f).forEach { level ->
      val tickY = bottomY - (cupInnerHeight * level * 0.92f)
      val t = (bottomY - tickY) / cupInnerHeight
      val rightEdgeAtY = bottomRightX + (topRightX - bottomRightX) * t
      drawLine(
        color = WaterCupStroke.copy(alpha = 0.45f),
        start = Offset(rightEdgeAtY - 11.dp.toPx(), tickY),
        end = Offset(rightEdgeAtY - 3.dp.toPx(), tickY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }

    // Glass rim & outer stroke
    drawPath(
      path = cupPath,
      color = WaterCupStroke,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Left glass shine streak
    drawLine(
      color = Color.White.copy(alpha = 0.65f),
      start = Offset(topLeftX + 8.dp.toPx(), topY + 12.dp.toPx()),
      end = Offset(bottomLeftX + 5.dp.toPx(), bottomY - 16.dp.toPx()),
      strokeWidth = 3.dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}
