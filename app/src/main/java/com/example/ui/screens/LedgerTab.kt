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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CashTransaction
import com.example.data.model.TransactionType
import com.example.domain.FinanceFormatter
import com.example.ui.theme.LuckyOrangeTertiary
import java.util.Calendar

private enum class LedgerFilter(val label: String) {
  ALL("Todos"),
  INCOME("Entradas"),
  EXPENSE("Saídas"),
}

@Composable
fun LedgerTab(
  transactions: List<CashTransaction>,
  onEditTransaction: (CashTransaction) -> Unit,
  onDeleteTransaction: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(LedgerFilter.ALL) }
  var selectedMonthKey by remember { mutableStateOf<String?>(null) }

  val availableMonths =
    remember(transactions) {
      transactions
        .map { tx ->
          val cal = FinanceFormatter.utcCalendar(tx.dateMillis)
          val y = cal.get(Calendar.YEAR)
          val m = cal.get(Calendar.MONTH) + 1
          val key = "$y-${m.toString().padStart(2, '0')}"
          key to FinanceFormatter.monthShort(y, m)
        }
        .distinctBy { it.first }
    }

  val filtered =
    remember(transactions, searchQuery, selectedFilter, selectedMonthKey) {
      transactions.filter { tx ->
        val matchesType =
          when (selectedFilter) {
            LedgerFilter.ALL -> true
            LedgerFilter.INCOME -> tx.transactionType == TransactionType.INCOME
            LedgerFilter.EXPENSE -> tx.transactionType == TransactionType.EXPENSE
          }
        val matchesQuery =
          searchQuery.isBlank() ||
            tx.description.contains(searchQuery, ignoreCase = true) ||
            tx.category.contains(searchQuery, ignoreCase = true)
        val matchesMonth =
          selectedMonthKey == null ||
            run {
              val cal = FinanceFormatter.utcCalendar(tx.dateMillis)
              val key =
                "${cal.get(Calendar.YEAR)}-${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}"
              key == selectedMonthKey
            }
        matchesType && matchesQuery && matchesMonth
      }
    }

  val filteredIn =
    filtered.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
  val filteredOut =
    filtered.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Livro Caixa",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Buscar por descrição ou categoria...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("ledger_search_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          LedgerFilter.entries.forEach { filter ->
            FilterChip(
              selected = selectedFilter == filter,
              onClick = { selectedFilter = filter },
              label = { Text(filter.label) },
            )
          }
        }

        if (availableMonths.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = selectedMonthKey == null,
                onClick = { selectedMonthKey = null },
                label = { Text("Todos os meses") },
              )
            }
            items(availableMonths, key = { it.first }) { (key, label) ->
              FilterChip(
                selected = selectedMonthKey == key,
                onClick = { selectedMonthKey = if (selectedMonthKey == key) null else key },
                label = { Text(label) },
              )
            }
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Entradas: ${FinanceFormatter.brl(filteredIn)}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Saídas: ${FinanceFormatter.brl(filteredOut)}",
              style = MaterialTheme.typography.labelLarge,
              color = LuckyOrangeTertiary,
            )
          }
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          title = "Nenhuma movimentação encontrada",
          subtitle = "Toque no botão + para registrar entradas ou saídas com data e descrição, sem limites.",
        )
      }
    } else {
      items(filtered, key = { it.id }) { tx ->
        TransactionRowCard(
          tx = tx,
          onEdit = { onEditTransaction(tx) },
          onDelete = { onDeleteTransaction(tx.id) },
        )
      }
    }
  }
}

@Composable
private fun TransactionRowCard(
  tx: CashTransaction,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val isIncome = tx.transactionType == TransactionType.INCOME
  val accentColor = if (isIncome) MaterialTheme.colorScheme.primary else LuckyOrangeTertiary

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("tx_card_${tx.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(40.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
          contentDescription = tx.transactionType.labelPt,
          tint = accentColor,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.description,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = FinanceFormatter.datePtBr(tx.dateMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (tx.category.isNotBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = tx.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${if (isIncome) "+" else "-"} ${FinanceFormatter.brl(tx.amount)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = accentColor,
        )
        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Editar movimentação",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Excluir movimentação",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
      ),
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(42.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
