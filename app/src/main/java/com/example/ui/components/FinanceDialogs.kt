package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CashTransaction
import com.example.data.model.DebtItem
import com.example.data.model.FinancialGoal
import com.example.data.model.TransactionType
import com.example.domain.FinanceFormatter

fun parseBrlInput(raw: String): Double? {
  val cleaned = raw.trim().replace("R$", "").replace(" ", "")
  if (cleaned.isBlank()) return null
  val normalized =
    if (cleaned.contains(",") && cleaned.contains(".")) {
      cleaned.replace(".", "").replace(",", ".")
    } else if (cleaned.contains(",")) {
      cleaned.replace(",", ".")
    } else {
      cleaned
    }
  return normalized.toDoubleOrNull()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormDialog(
  initial: CashTransaction?,
  onDismiss: () -> Unit,
  onSave: (type: TransactionType, amount: Double, description: String, category: String, dateMillis: Long) -> Unit,
) {
  var type by remember(initial) {
    mutableStateOf(initial?.transactionType ?: TransactionType.EXPENSE)
  }
  var amountText by remember(initial) {
    mutableStateOf(if (initial != null && initial.amount > 0) initial.amount.toString() else "")
  }
  var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
  var category by remember(initial) {
    mutableStateOf(
      initial?.category?.ifBlank { null }
        ?: if (type == TransactionType.INCOME) "Salário" else "Essencial"
    )
  }
  var dateMillis by remember(initial) {
    mutableLongStateOf(initial?.dateMillis ?: FinanceFormatter.todayUtcMidnightMillis())
  }
  var showDatePicker by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  val categoryOptions =
    if (type == TransactionType.INCOME) {
      listOf("Salário", "Renda Extra", "Vendas", "Investimentos", "Outros")
    } else {
      listOf("Essencial", "Lazer", "Dívidas e Invest.", "Transporte", "Saúde", "Outros")
    }

  if (showDatePicker) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { dateMillis = it }
            showDatePicker = false
          }
        ) {
          Text("Confirmar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initial == null) "Nova Movimentação" else "Editar Movimentação",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = type == TransactionType.INCOME,
            onClick = {
              type = TransactionType.INCOME
              if (category in listOf("Essencial", "Lazer", "Dívidas e Invest.")) {
                category = "Salário"
              }
            },
            label = { Text("Entrada (+)") },
            modifier = Modifier.weight(1f).testTag("chip_type_income"),
          )
          FilterChip(
            selected = type == TransactionType.EXPENSE,
            onClick = {
              type = TransactionType.EXPENSE
              if (category in listOf("Salário", "Renda Extra", "Vendas")) {
                category = "Essencial"
              }
            },
            label = { Text("Saída (-)") },
            modifier = Modifier.weight(1f).testTag("chip_type_expense"),
          )
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            errorText = null
          },
          label = { Text("Valor (R$)") },
          placeholder = { Text("Ex: 1500,00") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("input_tx_amount"),
        )

        OutlinedTextField(
          value = description,
          onValueChange = {
            if (it.length <= 120) description = it
            errorText = null
          },
          label = { Text("Descrição") },
          placeholder = { Text("Ex: Salário mensal, Supermercado...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_tx_description"),
        )

        OutlinedButton(
          onClick = { showDatePicker = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("btn_tx_date"),
        ) {
          Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Data: ${FinanceFormatter.datePtBr(dateMillis)}")
        }

        Column {
          Text(
            text = "Categoria",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            categoryOptions.forEach { opt ->
              FilterChip(
                selected = category == opt,
                onClick = { category = opt },
                label = { Text(opt) },
              )
            }
          }
        }

        if (errorText != null) {
          Text(
            text = errorText ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = parseBrlInput(amountText)
          when {
            parsed == null || parsed <= 0.0 -> errorText = "Informe um valor maior que zero."
            description.isBlank() -> errorText = "Informe uma descrição."
            else -> onSave(type, parsed, description.trim(), category, dateMillis)
          }
        },
        modifier = Modifier.testTag("btn_save_tx"),
      ) {
        Text("Salvar")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}

@Composable
fun GoalFormDialog(
  initial: FinancialGoal?,
  onDismiss: () -> Unit,
  onSave: (title: String, targetAmount: Double, notes: String) -> Unit,
) {
  var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
  var targetText by remember(initial) {
    mutableStateOf(
      if (initial != null && initial.targetAmount > 0) initial.targetAmount.toString() else ""
    )
  }
  var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initial == null) "Novo Objetivo" else "Editar Objetivo",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            if (it.length <= 100) title = it
            errorText = null
          },
          label = { Text("Nome do objetivo") },
          placeholder = { Text("Ex: Reserva de emergência, Viagem...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_goal_title"),
        )
        OutlinedTextField(
          value = targetText,
          onValueChange = {
            targetText = it
            errorText = null
          },
          label = { Text("Valor a arrecadar (R$)") },
          placeholder = { Text("Ex: 15000,00") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("input_goal_target"),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { if (it.length <= 200) notes = it },
          label = { Text("Observações (opcional)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
          Text(
            text = errorText ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = parseBrlInput(targetText)
          when {
            title.isBlank() -> errorText = "Informe o nome do objetivo."
            parsed == null || parsed <= 0.0 -> errorText = "Informe um valor alvo válido."
            else -> onSave(title.trim(), parsed, notes.trim())
          }
        },
        modifier = Modifier.testTag("btn_save_goal"),
      ) {
        Text("Salvar")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}

@Composable
fun DebtFormDialog(
  initial: DebtItem?,
  onDismiss: () -> Unit,
  onSave: (title: String, creditor: String, totalAmount: Double, paidAmount: Double) -> Unit,
) {
  var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
  var creditor by remember(initial) { mutableStateOf(initial?.creditor ?: "") }
  var totalText by remember(initial) {
    mutableStateOf(
      if (initial != null && initial.totalAmount > 0) initial.totalAmount.toString() else ""
    )
  }
  var paidText by remember(initial) {
    mutableStateOf(
      if (initial != null && initial.paidAmount > 0) initial.paidAmount.toString() else "0"
    )
  }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initial == null) "Nova Dívida" else "Editar Dívida",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            if (it.length <= 100) title = it
            errorText = null
          },
          label = { Text("Descrição da dívida") },
          placeholder = { Text("Ex: Cartão de crédito, Financiamento...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_debt_title"),
        )
        OutlinedTextField(
          value = creditor,
          onValueChange = { if (it.length <= 80) creditor = it },
          label = { Text("Credor (opcional)") },
          placeholder = { Text("Ex: Banco, Loja...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = totalText,
          onValueChange = {
            totalText = it
            errorText = null
          },
          label = { Text("Valor total da dívida (R$)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("input_debt_total"),
        )
        OutlinedTextField(
          value = paidText,
          onValueChange = {
            paidText = it
            errorText = null
          },
          label = { Text("Já pago (R$)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
          Text(
            text = errorText ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val total = parseBrlInput(totalText)
          val paid = parseBrlInput(paidText) ?: 0.0
          when {
            title.isBlank() -> errorText = "Informe o nome da dívida."
            total == null || total <= 0.0 -> errorText = "Informe um valor total maior que zero."
            paid < 0.0 || paid > total -> errorText = "O valor pago não pode exceder o total."
            else -> onSave(title.trim(), creditor.trim(), total, paid)
          }
        },
        modifier = Modifier.testTag("btn_save_debt"),
      ) {
        Text("Salvar")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}

@Composable
fun DebtPaymentDialog(
  debt: DebtItem,
  suggestedInstallment: Double,
  onDismiss: () -> Unit,
  onConfirmPayment: (Double) -> Unit,
) {
  var paymentText by remember(debt) {
    val defaultPay =
      if (suggestedInstallment > 0.0) minOf(suggestedInstallment, debt.remainingAmount)
      else debt.remainingAmount
    mutableStateOf(String.format(java.util.Locale.US, "%.2f", defaultPay))
  }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Abater Dívida", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${debt.title} • Restante: ${FinanceFormatter.brl(debt.remainingAmount)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = paymentText,
          onValueChange = {
            paymentText = it
            errorText = null
          },
          label = { Text("Valor do pagamento (R$)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("input_debt_payment"),
        )
        if (errorText != null) {
          Text(
            text = errorText ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = parseBrlInput(paymentText)
          when {
            parsed == null || parsed <= 0.0 -> errorText = "Informe um valor positivo."
            parsed > debt.remainingAmount + 0.01 ->
              errorText = "O valor excede o saldo restante da dívida."
            else -> onConfirmPayment(parsed.coerceAtMost(debt.remainingAmount))
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        Text("Confirmar")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}
