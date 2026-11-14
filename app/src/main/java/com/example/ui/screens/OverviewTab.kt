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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.FinanceFormatter
import com.example.domain.SmartSpreadsheetSummary
import com.example.ui.components.CashGrowthAreaChart
import com.example.ui.theme.LuckyGoldSecondary
import com.example.ui.theme.LuckyGreenDeep
import com.example.ui.theme.LuckyGreenPrimary
import com.example.ui.theme.LuckyOrangeDeep
import com.example.ui.theme.LuckyOrangeTertiary

@Composable
fun OverviewTab(
  summary: SmartSpreadsheetSummary,
  transactionCount: Int,
  onNavigateToLedger: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val avg = summary.averages

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Hero Balance Card
    item {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
              Brush.linearGradient(
                colors =
                  if (avg.cashBalance >= 0) listOf(LuckyGreenPrimary, LuckyGreenDeep)
                  else listOf(LuckyOrangeTertiary, LuckyOrangeDeep)
              )
            )
            .padding(20.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Saldo Atual em Caixa",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White.copy(alpha = 0.88f),
            )
            Surface(
              color = Color.White.copy(alpha = 0.16f),
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = "$transactionCount lançamentos",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = FinanceFormatter.brl(avg.cashBalance),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
          )

          Spacer(modifier = Modifier.height(18.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            HeroMetricPill(
              icon = Icons.AutoMirrored.Filled.TrendingUp,
              label = "Total Entradas",
              value = FinanceFormatter.brl(avg.totalIncome),
              modifier = Modifier.weight(1f),
            )
            HeroMetricPill(
              icon = Icons.AutoMirrored.Filled.TrendingDown,
              label = "Total Saídas",
              value = FinanceFormatter.brl(avg.totalExpense),
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // 2. Médias de Gastos (Diária, Semanal, Mensal, Anual)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Médias de Gastos & Fluxo",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            if (avg.activeDaysSpan > 0)
              "Baseado em ${avg.activeDaysSpan} dia(s) e ${avg.activeMonthsSpan} mês(es) no livro caixa"
            else "Adicione lançamentos no Livro Caixa para gerar as médias automáticas.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          AverageStatCard(
            icon = Icons.Filled.CalendarToday,
            label = "Média Diária",
            value = FinanceFormatter.brl(avg.dailyExpenseAvg),
            accent = LuckyOrangeTertiary,
            modifier = Modifier.weight(1f),
          )
          AverageStatCard(
            icon = Icons.Filled.DateRange,
            label = "Média Semanal",
            value = FinanceFormatter.brl(avg.weeklyExpenseAvg),
            accent = LuckyGoldSecondary,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          AverageStatCard(
            icon = Icons.Filled.CalendarMonth,
            label = "Média Mensal",
            value = FinanceFormatter.brl(avg.monthlyExpenseAvg),
            accent = LuckyOrangeTertiary,
            modifier = Modifier.weight(1f),
          )
          AverageStatCard(
            icon = Icons.Filled.EventRepeat,
            label = "Média Anual",
            value = FinanceFormatter.brl(avg.annualExpenseAvg),
            accent = LuckyGreenPrimary,
            modifier = Modifier.weight(1f),
          )
        }

        // Monthly average income & surplus summary strip
        Card(
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier =
                  Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.Savings,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(20.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Sobra Média Mensal",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = "Entradas médias: ${FinanceFormatter.brl(avg.monthlyIncomeAvg)}/mês",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
              }
            }
            Text(
              text = FinanceFormatter.brl(avg.monthlySurplusAvg),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color =
                if (avg.monthlySurplusAvg >= 0) LuckyGoldSecondary
                else LuckyOrangeTertiary,
            )
          }
        }
      }
    }

    // 3. Area Chart (Gráfico de Crescimento de Caixa)
    item {
      CashGrowthAreaChart(months = summary.allMonthsForChart)
    }

    // 4. Quick Insights Card (Goals & Debts snapshot)
    item {
      Card(
        onClick = onNavigateToLedger,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Resumo da Planilha",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text =
                "${summary.goalProjections.size} objetivo(s) cadastrado(s) • " +
                  "${summary.debtSuggestions.size} dívida(s) (${FinanceFormatter.brl(summary.totalRemainingDebt)} em aberto)",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeroMetricPill(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.White.copy(alpha = 0.14f),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.8f),
        )
        Text(
          text = value,
          style = MaterialTheme.typography.labelLarge,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun AverageStatCard(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(30.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
