package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.FinanceFormatter
import com.example.domain.MonthlyCashSnapshot
import com.example.ui.theme.LuckyOrangeTertiary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun CashGrowthAreaChart(
  months: List<MonthlyCashSnapshot>,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val projectionColor = MaterialTheme.colorScheme.secondary
  val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
  val surfaceColor = MaterialTheme.colorScheme.surface

  val lastRealIdx = remember(months) { months.indexOfLast { !it.isProjected }.coerceAtLeast(0) }
  var selectedIdx by remember(months) { mutableIntStateOf(lastRealIdx) }
  val safeSelectedIdx = selectedIdx.coerceIn(0, (months.size - 1).coerceAtLeast(0))

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Crescimento de Caixa",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Evolução mensal acumulada + projeção futura",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LegendDot(color = primaryColor, label = "Realizado")
        LegendDot(color = projectionColor, label = "Projetado")
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (months.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().height(160.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Registre entradas ou saídas para visualizar o gráfico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        val selected = months[safeSelectedIdx]
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text =
                  if (selected.isProjected) "${selected.fullLabel} (Projeção)"
                  else selected.fullLabel,
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (selected.isProjected) projectionColor else MaterialTheme.colorScheme.primary,
              )
              Text(
                text =
                  "Entradas: ${FinanceFormatter.brl(selected.inflows)} • Saídas: ${FinanceFormatter.brl(selected.outflows)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = FinanceFormatter.brl(selected.cumulativeCash),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                  if (selected.cumulativeCash >= 0) MaterialTheme.colorScheme.onSurface
                  else LuckyOrangeTertiary,
              )
              Text(
                text = "Caixa acumulado",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val values = months.map { it.cumulativeCash }
        val minVal = min(0.0, values.minOrNull() ?: 0.0)
        val maxVal = max(100.0, values.maxOrNull() ?: 100.0)
        val range = max(1.0, maxVal - minVal)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = FinanceFormatter.brlCompact(maxVal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Toque nos pontos para inspecionar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
          modifier =
            Modifier.fillMaxWidth()
              .height(185.dp)
              .pointerInput(months) {
                detectTapGestures { offset ->
                  val count = months.size
                  if (count > 0) {
                    val padH = 16.dp.toPx()
                    val usableW = (size.width - padH * 2).coerceAtLeast(1f)
                    val idx =
                      if (count == 1) 0
                      else
                        (((offset.x - padH) / usableW) * (count - 1))
                          .let { kotlin.math.round(it).toInt() }
                          .coerceIn(0, count - 1)
                    selectedIdx = idx
                  }
                }
              }
        ) {
          val padH = 16.dp.toPx()
          val padTop = 14.dp.toPx()
          val padBottom = 14.dp.toPx()
          val usableW = (size.width - padH * 2).coerceAtLeast(1f)
          val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)

          fun yFor(v: Double): Float {
            val norm = ((v - minVal) / range).toFloat().coerceIn(0f, 1f)
            return padTop + usableH * (1f - norm)
          }

          // Grid lines
          val gridCount = 3
          for (i in 0..gridCount) {
            val gy = padTop + usableH * (i / gridCount.toFloat())
            drawLine(
              color = outlineColor,
              start = Offset(padH, gy),
              end = Offset(size.width - padH, gy),
              strokeWidth = 1.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
          }

          // Zero baseline if negative values exist
          if (minVal < 0.0) {
            val zeroY = yFor(0.0)
            drawLine(
              color = LuckyOrangeTertiary.copy(alpha = 0.45f),
              start = Offset(padH, zeroY),
              end = Offset(size.width - padH, zeroY),
              strokeWidth = 1.5.dp.toPx(),
            )
          }

          val points =
            months.mapIndexed { idx, snap ->
              val x =
                if (months.size == 1) padH + usableW / 2f
                else padH + usableW * (idx.toFloat() / (months.size - 1))
              val y = yFor(snap.cumulativeCash)
              Offset(x, y)
            }

          val firstProjIdx = months.indexOfFirst { it.isProjected }
          val splitEnd =
            if (firstProjIdx == -1) points.lastIndex
            else (firstProjIdx - 1).coerceAtLeast(0)

          val baseLineY = padTop + usableH

          // Historical area + line
          if (splitEnd >= 0 && months.any { !it.isProjected }) {
            val histPoints = points.subList(0, splitEnd + 1)
            val areaPath =
              Path().apply {
                moveTo(histPoints.first().x, baseLineY)
                histPoints.forEach { pt -> lineTo(pt.x, pt.y) }
                lineTo(histPoints.last().x, baseLineY)
                close()
              }
            drawPath(
              path = areaPath,
              brush =
                Brush.verticalGradient(
                  colors = listOf(primaryColor.copy(alpha = 0.34f), primaryColor.copy(alpha = 0.02f)),
                  startY = padTop,
                  endY = baseLineY,
                ),
            )
            if (histPoints.size >= 2) {
              val linePath =
                Path().apply {
                  moveTo(histPoints.first().x, histPoints.first().y)
                  for (i in 1 until histPoints.size) {
                    val prev = histPoints[i - 1]
                    val curr = histPoints[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                  }
                }
              drawPath(
                path = linePath,
                color = primaryColor,
                style =
                  Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
            }
          }

          // Projected area + dashed line
          if (firstProjIdx != -1) {
            val startIdx = (firstProjIdx - 1).coerceAtLeast(0)
            val projPoints = points.subList(startIdx, points.size)
            if (projPoints.isNotEmpty()) {
              val projArea =
                Path().apply {
                  moveTo(projPoints.first().x, baseLineY)
                  projPoints.forEach { pt -> lineTo(pt.x, pt.y) }
                  lineTo(projPoints.last().x, baseLineY)
                  close()
                }
              drawPath(
                path = projArea,
                brush =
                  Brush.verticalGradient(
                    colors =
                      listOf(
                        projectionColor.copy(alpha = 0.24f),
                        projectionColor.copy(alpha = 0.02f),
                      ),
                    startY = padTop,
                    endY = baseLineY,
                  ),
              )
              if (projPoints.size >= 2) {
                val projLine =
                  Path().apply {
                    moveTo(projPoints.first().x, projPoints.first().y)
                    for (i in 1 until projPoints.size) {
                      lineTo(projPoints[i].x, projPoints[i].y)
                    }
                  }
                drawPath(
                  path = projLine,
                  color = projectionColor,
                  style =
                    Stroke(
                      width = 2.5.dp.toPx(),
                      cap = StrokeCap.Round,
                      pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                    ),
                )
              }
            }
          }

          // Data point dots + selected highlight
          points.forEachIndexed { idx, pt ->
            val snap = months[idx]
            val dotColor = if (snap.isProjected) projectionColor else primaryColor
            val isSelected = idx == safeSelectedIdx
            if (isSelected) {
              drawLine(
                color = dotColor.copy(alpha = 0.35f),
                start = Offset(pt.x, padTop),
                end = Offset(pt.x, baseLineY),
                strokeWidth = 1.5.dp.toPx(),
              )
              drawCircle(color = dotColor.copy(alpha = 0.22f), radius = 10.dp.toPx(), center = pt)
              drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = pt)
              drawCircle(color = dotColor, radius = 4.dp.toPx(), center = pt)
            } else {
              drawCircle(color = surfaceColor, radius = 4.dp.toPx(), center = pt)
              drawCircle(color = dotColor, radius = 2.8.dp.toPx(), center = pt)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          val step = max(1, months.size / 5)
          months.forEachIndexed { idx, snap ->
            if (idx == 0 || idx == months.lastIndex || idx % step == 0) {
              if (abs(idx - months.lastIndex) >= step / 2 || idx == months.lastIndex || idx == 0) {
                Text(
                  text = snap.label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (idx == safeSelectedIdx) FontWeight.Bold else FontWeight.Normal,
                  color =
                    if (idx == safeSelectedIdx) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
