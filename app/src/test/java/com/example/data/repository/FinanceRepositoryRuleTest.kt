package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addAndObserveTransactions_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FinanceRepository(firestore, auth)

    val id =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .addTransaction(
            type = TransactionType.INCOME,
            amount = 4200.0,
            description = "Salário",
            category = "Renda",
            dateMillis = 1763100000000L,
          )
          .getOrThrow()
      }
    assertTrue(id.isNotEmpty())

    val list =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTransactions(uid).first { txs -> txs.any { it.id == id } }
      }
    val item = list.first { it.id == id }
    assertEquals("Salário", item.description)
    assertEquals(4200.0, item.amount, 0.001)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateTransaction(item.copy(amount = 4500.0)).getOrThrow()
      repo.deleteTransaction(id).getOrThrow()
    }
  }

  @Test
  fun addAndObserveGoalsAndDebts_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FinanceRepository(firestore, auth)

    val goalId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addGoal("Reserva", 15000.0, "Fundo de segurança").getOrThrow()
      }
    val debtId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addDebt("Empréstimo", "Banco", 3000.0, 500.0).getOrThrow()
      }

    val goals =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeGoals(uid).first { list -> list.any { it.id == goalId } }
      }
    val debts =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDebts(uid).first { list -> list.any { it.id == debtId } }
      }
    assertEquals(15000.0, goals.first { it.id == goalId }.targetAmount, 0.001)
    assertEquals(2500.0, debts.first { it.id == debtId }.remainingAmount, 0.001)
  }

  @Test
  fun observeTransactions_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FinanceRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo
        .addTransaction(
          type = TransactionType.EXPENSE,
          amount = 150.0,
          description = "Mercado",
          category = "Essencial",
          dateMillis = 1763100000000L,
        )
        .getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = FinanceRepository(firestore, auth)
    val error =
      try {
        withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeTransactions(aliceUid).first() }
        null
      } catch (e: Exception) {
        e
      }
    checkNotNull(error) { "Expected PERMISSION_DENIED when Bob observes Alice's transactions" }
    val firestoreError = checkNotNull(error.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $error"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreError.code)
  }

  @Test
  fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = FinanceRepository(firestore, auth)

    val error =
      try {
        withTimeout(FLOW_TIMEOUT_MS) { repo.observeTransactions("some_uid").first() }
        null
      } catch (e: Exception) {
        e
      }
    checkNotNull(error) { "Expected PERMISSION_DENIED when unauthenticated" }
    val firestoreError = checkNotNull(error.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $error"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreError.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_caixa@test.com"
    const val BOB_EMAIL = "bob_caixa@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
