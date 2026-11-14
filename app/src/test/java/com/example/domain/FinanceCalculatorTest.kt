package com.example.domain

import com.example.data.model.CashTransaction
import com.example.data.model.DebtItem
import com.example.data.model.FinancialGoal
import com.example.data.model.TransactionType
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculatorTest {

  private fun utcMillis(year: Int, month1To12: Int, day: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(year, month1To12 - 1, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
  }

  @Test
  fun compute_calculatesAveragesProjectionsGoalsAndDebtInstallments() {
    val txs =
      listOf(
        CashTransaction(
          id = "1",
          type = TransactionType.INCOME.value,
          amount = 5000.0,
          description = "Salário Jan",
          dateMillis = utcMillis(2026, 1, 5),
        ),
        CashTransaction(
          id = "2",
          type = TransactionType.EXPENSE.value,
          amount = 2000.0,
          description = "Contas Jan",
          dateMillis = utcMillis(2026, 1, 15),
        ),
        CashTransaction(
          id = "3",
          type = TransactionType.INCOME.value,
          amount = 5000.0,
          description = "Salário Fev",
          dateMillis = utcMillis(2026, 2, 5),
        ),
        CashTransaction(
          id = "4",
          type = TransactionType.EXPENSE.value,
          amount = 3000.0,
          description = "Contas Fev",
          dateMillis = utcMillis(2026, 2, 5),
        ),
      )

    val goals =
      listOf(
        FinancialGoal(id = "g1", title = "Reserva", targetAmount = 10000.0),
        FinancialGoal(id = "g2", title = "Celular", targetAmount = 4000.0),
      )

    val debts =
      listOf(
        DebtItem(id = "d1", title = "Cartão", totalAmount = 1800.0, paidAmount = 300.0)
      )

    val summary = FinanceCalculator.compute(txs, goals, debts, utcMillis(2026, 2, 10))

    // Total Income = 10000, Total Expense = 5000, Cash = 5000
    assertEquals(10000.0, summary.averages.totalIncome, 0.01)
    assertEquals(5000.0, summary.averages.totalExpense, 0.01)
    assertEquals(5000.0, summary.averages.cashBalance, 0.01)
    assertEquals(2, summary.averages.activeMonthsSpan)
    // Monthly avg expense = 5000 / 2 = 2500; Annual = 30000
    assertEquals(2500.0, summary.averages.monthlyExpenseAvg, 0.01)
    assertEquals(30000.0, summary.averages.annualExpenseAvg, 0.01)

    // Historical months: Jan (surplus 3000 -> cum 3000), Feb (surplus 2000 -> cum 5000)
    assertEquals(2, summary.historicalMonths.size)
    assertFalse(summary.historicalMonths[0].isProjected)
    assertEquals(1500.0, summary.historicalMonths[0].essential50, 0.01)
    assertEquals(900.0, summary.historicalMonths[0].leisure30, 0.01)
    assertEquals(600.0, summary.historicalMonths[0].debtsInvest20, 0.01)

    // Projected months: 6 future months
    assertEquals(6, summary.projectedMonths.size)
    assertTrue(summary.projectedMonths.all { it.isProjected })
    assertEquals(7500.0, summary.projectedMonths[0].cumulativeCash, 0.01)

    // Goals: g1 needs 10000 - 5000 = 5000 remaining; g2 achieved
    val g1 = summary.goalProjections.first { it.goal.id == "g1" }
    assertEquals(5000.0, g1.remainingAmount, 0.01)
    assertEquals(2, g1.estimatedMonths)
    val g2 = summary.goalProjections.first { it.goal.id == "g2" }
    assertTrue(g2.isAchieved)
    assertEquals(0.0, g2.remainingAmount, 0.01)

    // Debts: avg 20% = (600 + 400) / 2 = 500/month. Remaining debt = 1500 -> 3 months!
    assertEquals(500.0, summary.maxMonthlyDebtInstallment20, 0.01)
    val d1 = summary.debtSuggestions.first()
    assertEquals(1500.0, d1.remainingDebt, 0.01)
    assertNotNull(d1.suggestedMonths)
    assertEquals(3, d1.suggestedMonths)
  }
}
