package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CashTransaction
import com.example.data.model.DebtItem
import com.example.data.model.FinancialGoal
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Usuário precisa estar autenticado com o Google.")

  // --- Transactions (Livro Caixa) ---

  fun observeTransactions(userId: String): Flow<List<CashTransaction>> = flow {
    val path = "users/$userId/transactions"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("transactions")
        .orderBy("dateMillis", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            CashTransaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logStreamErrorIfActive(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun addTransaction(
    type: TransactionType,
    amount: Double,
    description: String,
    category: String,
    dateMillis: Long,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("transactions").document()
    val payload =
      mapOf(
        "userId" to uid,
        "type" to type.value,
        "amount" to amount,
        "description" to description.trim(),
        "category" to category.trim(),
        "dateMillis" to dateMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateTransaction(transaction: CashTransaction): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(transaction.id.isNotBlank()) { "ID da movimentação inválido." }
    val docRef =
      db.collection("users").document(uid).collection("transactions").document(transaction.id)
    val updates =
      mapOf(
        "type" to transaction.type,
        "amount" to transaction.amount,
        "description" to transaction.description.trim(),
        "category" to transaction.category.trim(),
        "dateMillis" to transaction.dateMillis,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteTransaction(transactionId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef =
      db.collection("users").document(uid).collection("transactions").document(transactionId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- Financial Goals (Objetivos) ---

  fun observeGoals(userId: String): Flow<List<FinancialGoal>> = flow {
    val path = "users/$userId/goals"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("goals")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            FinancialGoal::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logStreamErrorIfActive(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun addGoal(title: String, targetAmount: Double, notes: String): Result<String> =
    runCatching {
      val uid = requireUserId()
      val docRef = db.collection("users").document(uid).collection("goals").document()
      val payload =
        mapOf(
          "userId" to uid,
          "title" to title.trim(),
          "targetAmount" to targetAmount,
          "notes" to notes.trim(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      try {
        docRef.set(payload).await()
        docRef.id
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }

  suspend fun updateGoal(goal: FinancialGoal): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(goal.id.isNotBlank()) { "ID do objetivo inválido." }
    val docRef = db.collection("users").document(uid).collection("goals").document(goal.id)
    val updates =
      mapOf(
        "title" to goal.title.trim(),
        "targetAmount" to goal.targetAmount,
        "notes" to goal.notes.trim(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteGoal(goalId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- Debts (Dívidas) ---

  fun observeDebts(userId: String): Flow<List<DebtItem>> = flow {
    val path = "users/$userId/debts"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("debts")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            DebtItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logStreamErrorIfActive(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun addDebt(
    title: String,
    creditor: String,
    totalAmount: Double,
    paidAmount: Double = 0.0,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("debts").document()
    val payload =
      mapOf(
        "userId" to uid,
        "title" to title.trim(),
        "creditor" to creditor.trim(),
        "totalAmount" to totalAmount,
        "paidAmount" to paidAmount.coerceIn(0.0, totalAmount),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateDebt(debt: DebtItem): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(debt.id.isNotBlank()) { "ID da dívida inválido." }
    val docRef = db.collection("users").document(uid).collection("debts").document(debt.id)
    val updates =
      mapOf(
        "title" to debt.title.trim(),
        "creditor" to debt.creditor.trim(),
        "totalAmount" to debt.totalAmount,
        "paidAmount" to debt.paidAmount.coerceIn(0.0, debt.totalAmount),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteDebt(debtId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("debts").document(debtId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  private fun logStreamErrorIfActive(
    error: Throwable,
    op: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(toLog, op, path)
    }
  }
}
