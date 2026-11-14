package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.FinanceFormatter
import com.example.domain.MonthlyCashSnapshot
import com.example.domain.SmartSpreadsheetSummary
import com.example.ui.theme.LuckyOrangeTertiary

private enum class DivisionFilter(val label: String) {
  ALL("Todos"),
  FILLED("Meses Preenchidos"),
  PROJECTED("Projeções Futuras"),
}

@Composable
fun DivisionTab(
  summary: SmartSpreadsheetSummary,
  modifier: Modifier = Modifier,
) {
  var filter by remember { mutableStateOf(DivisionFilter.ALL) }

  val monthsToShow =
    remember(summary, filter) {
      when (filter) {
        DivisionFilter.ALL -> summary.historicalMonths + summary.projectedMonths
        DivisionFilter.FILLED -> summary.historicalMonths
        DivisionFilter.PROJECTED -> summary.projectedMonths
      }
    }

  val avgMonthlyAllocatable = summary.averages.monthlySurplusAvg.coerceAtLeast(0.0)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Divisão Inteligente 50 / 30 / 20",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            "Sugestão mensal do que resta em caixa (Entradas - Saídas) para cada mês preenchido e projeções automáticas para os meses futuros.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Average 50/30/20 allocation reference banner
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Referência pela Sobra Média Mensal",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Base mensal média: ${FinanceFormatter.brl(avgMonthlyAllocatable)}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AllocationRatioBar()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              BucketPill(
                title = "50% Essencial",
                value = FinanceFormatter.brl(avgMonthlyAllocatable * 0.50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
              )
              BucketPill(
                title = "30% Lazer",
                value = FinanceFormatter.brl(avgMonthlyAllocatable * 0.30),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
              )
              BucketPill(
                title = "20% Dív./Inv.",
                value = FinanceFormatter.brl(avgMonthlyAllocatable * 0.20),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          DivisionFilter.entries.forEach { option ->
            FilterChip(
              selected = filter == option,
              onClick = { filter = option },
              label = { Text(option.label) },
            )
          }
        }
      }
    }

    if (monthsToShow.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.PieChart,
          title = "Sem meses preenchidos ainda",
          subtitle = "Adicione entradas e saídas no Livro Caixa para gerar a divisão 50/30/20 mensal e as projeções.",
        )
      }
    } else {
      items(monthsToShow, key = { "${it.year}-${it.month}-${it.isProjected}" }) { snap ->
        MonthlyDivisionCard(snap = snap)
      }
    }
  }
}

@Composable
private fun AllocationRatioBar() {
  Row(
    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Box(modifier = Modifier.weight(0.50f).fillMaxSize().background(MaterialTheme.colorScheme.primary))
    Box(modifier = Modifier.weight(0.30f).fillMaxSize().background(MaterialTheme.colorScheme.tertiary))
    Box(modifier = Modifier.weight(0.20f).fillMaxSize().background(MaterialTheme.colorScheme.secondary))
  }
}

@Composable
private fun BucketPill(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = color.copy(alpha = 0.11f),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun MonthlyDivisionCard(snap: MonthlyCashSnapshot) {
  val surplusColor =
    if (snap.surplus >= 0) MaterialTheme.colorScheme.primary else LuckyOrangeTertiary

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (snap.isProjected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (snap.isProjected) 0.dp else 1.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = snap.fullLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color =
              if (snap.isProjected) MaterialTheme.colorScheme.secondaryContainer
              else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = if (snap.isProjected) "Projeção" else "Realizado",
              style = MaterialTheme.typography.labelSmall,
              color =
                if (snap.isProjected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
        Text(
          text = "Sobra: ${FinanceFormatter.brl(snap.surplus)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = surplusColor,
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          "Entradas: ${FinanceFormatter.brl(snap.inflows)} • Saídas: ${FinanceFormatter.brl(snap.outflows)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (snap.surplus <= 0.0) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Sem saldo positivo neste mês para distribuir na regra 50/30/20.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(10.dp),
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BucketPill(
            title = "50% Essencial",
            value = FinanceFormatter.brl(snap.essential50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
          )
          BucketPill(
            title = "30% Lazer",
            value = FinanceFormatter.brl(snap.leisure30),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
          )
          BucketPill(
            title = "20% Dív./Inv.",
            value = FinanceFormatter.brl(snap.debtsInvest20),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}
