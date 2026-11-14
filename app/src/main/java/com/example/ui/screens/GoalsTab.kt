package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FinancialGoal
import com.example.domain.FinanceFormatter
import com.example.domain.GoalProjection
import com.example.domain.SmartSpreadsheetSummary
import com.example.ui.theme.LuckyOrangeTertiary

@Composable
fun GoalsTab(
  summary: SmartSpreadsheetSummary,
  onEditGoal: (FinancialGoal) -> Unit,
  onDeleteGoal: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val avg = summary.averages

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Objetivos Financeiros",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            "Cálculo automático de quanto falta para cada meta com base no seu caixa atual, entradas, saídas e sobra média mensal.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "Caixa Atual",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = FinanceFormatter.brl(avg.cashBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Sobra Média / Mês",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = FinanceFormatter.brl(avg.monthlySurplusAvg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }
    }

    if (summary.goalProjections.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Flag,
          title = "Nenhum objetivo cadastrado",
          subtitle = "Toque em + para adicionar um objetivo e o valor necessário para conquistá-lo.",
        )
      }
    } else {
      items(summary.goalProjections, key = { it.goal.id }) { projection ->
        GoalCard(
          projection = projection,
          monthlySurplusAvg = avg.monthlySurplusAvg,
          onEdit = { onEditGoal(projection.goal) },
          onDelete = { onDeleteGoal(projection.goal.id) },
        )
      }
    }
  }
}

@Composable
private fun GoalCard(
  projection: GoalProjection,
  monthlySurplusAvg: Double,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val goal = projection.goal
  val pct = (projection.progressFraction * 100f).toInt()

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("goal_card_${goal.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (projection.isAchieved) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Meta atingida",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
            Text(
              text = goal.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          if (goal.notes.isNotBlank()) {
            Text(
              text = goal.notes,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Editar objetivo",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Excluir objetivo",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      LinearProgressIndicator(
        progress = { projection.progressFraction },
        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "Valor da meta ($pct%)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = FinanceFormatter.brl(goal.targetAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = if (projection.isAchieved) "Situação" else "Quanto falta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text =
              if (projection.isAchieved) "Meta atingida!"
              else FinanceFormatter.brl(projection.remainingAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color =
              if (projection.isAchieved) MaterialTheme.colorScheme.primary
              else LuckyOrangeTertiary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        val estimateMsg =
          when {
            projection.isAchieved -> "Seu caixa atual já cobre integralmente este objetivo."
            projection.estimatedMonths != null ->
              "No ritmo atual (${FinanceFormatter.brl(monthlySurplusAvg)}/mês de sobra), faltam cerca de ${projection.estimatedMonths} mês(es)."
            else ->
              "Aumente sua sobra média mensal para calcular o prazo estimado até a meta."
          }
        Text(
          text = estimateMsg,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(10.dp),
        )
      }
    }
  }
}
