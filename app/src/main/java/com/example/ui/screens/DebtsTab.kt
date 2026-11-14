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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.model.DebtItem
import com.example.domain.DebtSuggestion
import com.example.domain.FinanceFormatter
import com.example.domain.SmartSpreadsheetSummary
import com.example.ui.theme.LuckyOrangeTertiary

@Composable
fun DebtsTab(
  summary: SmartSpreadsheetSummary,
  onEditDebt: (DebtItem) -> Unit,
  onPayDebt: (DebtItem, Double) -> Unit,
  onDeleteDebt: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Dívidas & Parcelamento Inteligente",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            "Parcela máxima calculada pela média mensal do espaço de 20% (Dívidas e Investimentos) da sobra do seu caixa.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Parcela Máx. Sugerida (20%)",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                  text = "${FinanceFormatter.brl(summary.maxMonthlyDebtInstallment20)} / mês",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "Dívida Total Restante",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                  text = FinanceFormatter.brl(summary.totalRemainingDebt),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = LuckyOrangeTertiary,
                )
              }
            }
            if (summary.totalRemainingDebt > 0.0) {
              Spacer(modifier = Modifier.height(8.dp))
              val combinedText =
                summary.combinedDebtPayoffMonths?.let { months ->
                  "Quitação total estimada (todas as dívidas): ~$months mês(es) aplicando a parcela de 20%."
                }
                  ?: "Registre meses com sobra positiva no Livro Caixa para gerar o plano de parcelamento."
              Text(
                text = combinedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }
      }
    }

    if (summary.debtSuggestions.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.CreditScore,
          title = "Nenhuma dívida registrada",
          subtitle = "Toque em + para anotar suas dívidas e ver em quantos meses quitá-las com os 20% do caixa.",
        )
      }
    } else {
      items(summary.debtSuggestions, key = { it.debt.id }) { suggestion ->
        DebtCard(
          suggestion = suggestion,
          onEdit = { onEditDebt(suggestion.debt) },
          onPay = { onPayDebt(suggestion.debt, suggestion.suggestedInstallment) },
          onDelete = { onDeleteDebt(suggestion.debt.id) },
        )
      }
    }
  }
}

@Composable
private fun DebtCard(
  suggestion: DebtSuggestion,
  onEdit: () -> Unit,
  onPay: () -> Unit,
  onDelete: () -> Unit,
) {
  val debt = suggestion.debt
  val isPaidOff = suggestion.remainingDebt <= 0.0
  val paidRatio =
    if (debt.totalAmount > 0.0) (debt.paidAmount / debt.totalAmount).coerceIn(0.0, 1.0).toFloat()
    else 1f

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("debt_card_${debt.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isPaidOff) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Dívida quitada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
            Text(
              text = debt.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          if (debt.creditor.isNotBlank()) {
            Text(
              text = "Credor: ${debt.creditor}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Editar dívida",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Excluir dívida",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      LinearProgressIndicator(
        progress = { paidRatio },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
        color = if (isPaidOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "Total (${FinanceFormatter.brl(debt.paidAmount)} pago)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = FinanceFormatter.brl(debt.totalAmount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Restante",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = if (isPaidOff) "Quitada!" else FinanceFormatter.brl(suggestion.remainingDebt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPaidOff) MaterialTheme.colorScheme.primary else LuckyOrangeTertiary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Sugestão de Parcelamento",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          val planText =
            when {
              isPaidOff -> "Dívida totalmente quitada."
              suggestion.suggestedMonths != null && suggestion.suggestedMonths == 1 ->
                "Pode quitar em 1x de ${FinanceFormatter.brl(suggestion.remainingDebt)} (dentro da parcela máxima de ${FinanceFormatter.brl(suggestion.maxInstallment20)}/mês)."
              suggestion.suggestedMonths != null ->
                "Pague em ${suggestion.suggestedMonths} meses: ${suggestion.suggestedMonths - 1}x de ${FinanceFormatter.brl(suggestion.suggestedInstallment)} + última de ${FinanceFormatter.brl(suggestion.lastInstallment)}."
              else ->
                "Sem capacidade de parcela nos 20% ainda (sobra média mensal de caixa zerada ou negativa)."
            }
          Text(
            text = planText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (!isPaidOff) {
        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(
          onClick = onPay,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Icon(imageVector = Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Registrar Pagamento / Abater")
        }
      }
    }
  }
}
