package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType(val value: String, val labelPt: String) {
  INCOME("income", "Entrada"),
  EXPENSE("expense", "Saída");

  companion object {
    fun fromValue(value: String): TransactionType =
      entries.firstOrNull { it.value == value } ?: EXPENSE
  }
}

@IgnoreExtraProperties
data class CashTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = TransactionType.EXPENSE.value,
  val amount: Double = 0.0,
  val description: String = "",
  val category: String = "",
  val dateMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val transactionType: TransactionType
    get() = TransactionType.fromValue(type)
}

@IgnoreExtraProperties
data class FinancialGoal(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val targetAmount: Double = 0.0,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DebtItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val creditor: String = "",
  val totalAmount: Double = 0.0,
  val paidAmount: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val remainingAmount: Double
    get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
}
