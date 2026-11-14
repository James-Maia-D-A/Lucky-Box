package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CashTransaction
import com.example.data.model.DebtItem
import com.example.data.model.FinancialGoal
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.domain.FinanceCalculator
import com.example.domain.SmartSpreadsheetSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FinanceUiState {
  data object Loading : FinanceUiState

  data class Ready(
    val summary: SmartSpreadsheetSummary,
    val transactions: List<CashTransaction>,
    val goals: List<FinancialGoal>,
    val debts: List<DebtItem>,
  ) : FinanceUiState

  data class Error(val message: String) : FinanceUiState
}

class FinanceViewModel(
  private val repository: FinanceRepository,
  private val currentUserId: String,
) : ViewModel() {

  val uiState: StateFlow<FinanceUiState> =
    combine(
        repository.observeTransactions(currentUserId),
        repository.observeGoals(currentUserId),
        repository.observeDebts(currentUserId),
      ) { transactions, goals, debts ->
        val summary = FinanceCalculator.compute(transactions, goals, debts)
        FinanceUiState.Ready(
          summary = summary,
          transactions = transactions,
          goals = goals,
          debts = debts,
        ) as FinanceUiState
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Erro ao observar dados financeiros", rootError)
        emit(
          FinanceUiState.Error(
            rootError.localizedMessage ?: "Não foi possível carregar sua planilha financeira."
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FinanceUiState.Loading,
      )

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun addTransaction(
    type: TransactionType,
    amount: Double,
    description: String,
    category: String,
    dateMillis: Long,
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      repository
        .addTransaction(type, amount, description, category, dateMillis)
        .onSuccess { _feedbackMessage.value = "Movimentação registrada." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao salvar movimentação."
        }
      _isSaving.value = false
    }
  }

  fun updateTransaction(transaction: CashTransaction) {
    viewModelScope.launch {
      _isSaving.value = true
      repository
        .updateTransaction(transaction)
        .onSuccess { _feedbackMessage.value = "Movimentação atualizada." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar movimentação."
        }
      _isSaving.value = false
    }
  }

  fun deleteTransaction(id: String) {
    viewModelScope.launch {
      repository
        .deleteTransaction(id)
        .onSuccess { _feedbackMessage.value = "Movimentação excluída." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao excluir movimentação."
        }
    }
  }

  fun addGoal(title: String, targetAmount: Double, notes: String) {
    viewModelScope.launch {
      _isSaving.value = true
      repository
        .addGoal(title, targetAmount, notes)
        .onSuccess { _feedbackMessage.value = "Objetivo adicionado." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao adicionar objetivo."
        }
      _isSaving.value = false
    }
  }

  fun updateGoal(goal: FinancialGoal) {
    viewModelScope.launch {
      _isSaving.value = true
      repository
        .updateGoal(goal)
        .onSuccess { _feedbackMessage.value = "Objetivo atualizado." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar objetivo."
        }
      _isSaving.value = false
    }
  }

  fun deleteGoal(id: String) {
    viewModelScope.launch {
      repository
        .deleteGoal(id)
        .onSuccess { _feedbackMessage.value = "Objetivo removido." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao remover objetivo."
        }
    }
  }

  fun addDebt(title: String, creditor: String, totalAmount: Double, paidAmount: Double) {
    viewModelScope.launch {
      _isSaving.value = true
      repository
        .addDebt(title, creditor, totalAmount, paidAmount)
        .onSuccess { _feedbackMessage.value = "Dívida registrada." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao registrar dívida."
        }
      _isSaving.value = false
    }
  }

  fun updateDebt(debt: DebtItem) {
    viewModelScope.launch {
      _isSaving.value = true
      repository
        .updateDebt(debt)
        .onSuccess { _feedbackMessage.value = "Dívida atualizada." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao atualizar dívida."
        }
      _isSaving.value = false
    }
  }

  fun registerDebtPayment(debt: DebtItem, paymentAmount: Double) {
    val updatedPaid = (debt.paidAmount + paymentAmount).coerceIn(0.0, debt.totalAmount)
    updateDebt(debt.copy(paidAmount = updatedPaid))
  }

  fun deleteDebt(id: String) {
    viewModelScope.launch {
      repository
        .deleteDebt(id)
        .onSuccess { _feedbackMessage.value = "Dívida excluída." }
        .onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Erro ao excluir dívida."
        }
    }
  }

  private companion object {
    const val TAG = "FinanceVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
