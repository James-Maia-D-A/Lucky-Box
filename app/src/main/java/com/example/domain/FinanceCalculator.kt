package com.example.domain

import com.example.data.model.CashTransaction
import com.example.data.model.DebtItem
import com.example.data.model.FinancialGoal
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class SpendingAverages(
  val totalIncome: Double = 0.0,
  val totalExpense: Double = 0.0,
  val cashBalance: Double = 0.0,
  val dailyExpenseAvg: Double = 0.0,
  val weeklyExpenseAvg: Double = 0.0,
  val monthlyExpenseAvg: Double = 0.0,
  val annualExpenseAvg: Double = 0.0,
  val monthlyIncomeAvg: Double = 0.0,
  val monthlySurplusAvg: Double = 0.0,
  val activeDaysSpan: Int = 0,
  val activeMonthsSpan: Int = 0,
)

data class MonthlyCashSnapshot(
  val year: Int,
  val month: Int, // 1..12
  val label: String,
  val fullLabel: String,
  val inflows: Double,
  val outflows: Double,
  val surplus: Double,
  val cumulativeCash: Double,
  val essential50: Double,
  val leisure30: Double,
  val debtsInvest20: Double,
  val isProjected: Boolean,
)

data class GoalProjection(
  val goal: FinancialGoal,
  val currentCash: Double,
  val remainingAmount: Double,
  val progressFraction: Float,
  val estimatedMonths: Int?,
  val isAchieved: Boolean,
)

data class DebtSuggestion(
  val debt: DebtItem,
  val remainingDebt: Double,
  val maxInstallment20: Double,
  val suggestedMonths: Int?,
  val suggestedInstallment: Double,
  val lastInstallment: Double,
)

data class SmartSpreadsheetSummary(
  val averages: SpendingAverages = SpendingAverages(),
  val historicalMonths: List<MonthlyCashSnapshot> = emptyList(),
  val projectedMonths: List<MonthlyCashSnapshot> = emptyList(),
  val allMonthsForChart: List<MonthlyCashSnapshot> = emptyList(),
  val goalProjections: List<GoalProjection> = emptyList(),
  val debtSuggestions: List<DebtSuggestion> = emptyList(),
  val maxMonthlyDebtInstallment20: Double = 0.0,
  val totalRemainingDebt: Double = 0.0,
  val combinedDebtPayoffMonths: Int? = null,
)

object FinanceFormatter {
  private val ptBr = Locale.forLanguageTag("pt-BR")
  private val currencyFormat: NumberFormat
    get() = NumberFormat.getCurrencyInstance(ptBr)

  private val monthShortNames =
    arrayOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
  private val monthFullNames =
    arrayOf(
      "Janeiro",
      "Fevereiro",
      "Março",
      "Abril",
      "Maio",
      "Junho",
      "Julho",
      "Agosto",
      "Setembro",
      "Outubro",
      "Novembro",
      "Dezembro",
    )

  fun brl(value: Double): String = currencyFormat.format(value)

  fun brlCompact(value: Double): String {
    val absVal = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
      absVal >= 1_000_000.0 -> String.format(ptBr, "%sR$ %.1fM", sign, absVal / 1_000_000.0)
      absVal >= 1_000.0 -> String.format(ptBr, "%sR$ %.1fk", sign, absVal / 1_000.0)
      else -> String.format(ptBr, "%sR$ %.0f", sign, absVal)
    }
  }

  fun datePtBr(utcMillis: Long): String {
    val cal = utcCalendar(utcMillis)
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH) + 1
    val y = cal.get(Calendar.YEAR)
    return String.format(ptBr, "%02d/%02d/%04d", d, m, y)
  }

  fun monthShort(year: Int, month1To12: Int): String {
    val mIdx = (month1To12 - 1).coerceIn(0, 11)
    return "${monthShortNames[mIdx]}/${(year % 100).toString().padStart(2, '0')}"
  }

  fun monthFull(year: Int, month1To12: Int): String {
    val mIdx = (month1To12 - 1).coerceIn(0, 11)
    return "${monthFullNames[mIdx]} $year"
  }

  fun todayUtcMidnightMillis(): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    utc.set(Calendar.MILLISECOND, 0)
    return utc.timeInMillis
  }

  fun normalizeToUtcMidnight(millis: Long): Long {
    val utc = utcCalendar(millis)
    utc.set(Calendar.HOUR_OF_DAY, 0)
    utc.set(Calendar.MINUTE, 0)
    utc.set(Calendar.SECOND, 0)
    utc.set(Calendar.MILLISECOND, 0)
    return utc.timeInMillis
  }

  fun utcCalendar(millis: Long): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
}

object FinanceCalculator {
  private const val MILLIS_PER_DAY = 86_400_000L
  private const val PROJECTION_MONTHS_COUNT = 6

  fun compute(
    transactions: List<CashTransaction>,
    goals: List<FinancialGoal>,
    debts: List<DebtItem>,
    referenceNowMillis: Long = FinanceFormatter.todayUtcMidnightMillis(),
  ): SmartSpreadsheetSummary {
    val totalIncome =
      transactions.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense =
      transactions.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
    val cashBalance = totalIncome - totalExpense

    if (transactions.isEmpty()) {
      val refCal = FinanceFormatter.utcCalendar(referenceNowMillis)
      val curY = refCal.get(Calendar.YEAR)
      val curM = refCal.get(Calendar.MONTH) + 1
      val emptyProjections = buildProjectedMonths(curY, curM, 0.0, 0.0, 0.0, PROJECTION_MONTHS_COUNT)
      return SmartSpreadsheetSummary(
        averages = SpendingAverages(),
        historicalMonths = emptyList(),
        projectedMonths = emptyProjections,
        allMonthsForChart = emptyProjections,
        goalProjections =
          goals.map { goal ->
            GoalProjection(
              goal = goal,
              currentCash = 0.0,
              remainingAmount = goal.targetAmount,
              progressFraction = 0f,
              estimatedMonths = null,
              isAchieved = false,
            )
          },
        debtSuggestions =
          debts.map { debt ->
            DebtSuggestion(
              debt = debt,
              remainingDebt = debt.remainingAmount,
              maxInstallment20 = 0.0,
              suggestedMonths = if (debt.remainingAmount <= 0.0) 0 else null,
              suggestedInstallment = 0.0,
              lastInstallment = 0.0,
            )
          },
        maxMonthlyDebtInstallment20 = 0.0,
        totalRemainingDebt = debts.sumOf { it.remainingAmount },
        combinedDebtPayoffMonths = null,
      )
    }

    val normalizedDates = transactions.map { FinanceFormatter.normalizeToUtcMidnight(it.dateMillis) }
    val minDay = normalizedDates.min()
    val maxDay = normalizedDates.max()
    val spanDays = max(1, ((maxDay - minDay) / MILLIS_PER_DAY).toInt() + 1)

    val minCal = FinanceFormatter.utcCalendar(minDay)
    val maxCal = FinanceFormatter.utcCalendar(maxDay)
    val minYear = minCal.get(Calendar.YEAR)
    val minMonth = minCal.get(Calendar.MONTH) + 1
    val maxYear = maxCal.get(Calendar.YEAR)
    val maxMonth = maxCal.get(Calendar.MONTH) + 1
    val spanMonths = max(1, ((maxYear - minYear) * 12 + (maxMonth - minMonth)) + 1)

    // Group transactions by (year, month)
    val monthlyGrouped =
      transactions.groupBy { tx ->
        val cal = FinanceFormatter.utcCalendar(tx.dateMillis)
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
      }

    val historical = mutableListOf<MonthlyCashSnapshot>()
    var runningCash = 0.0
    var cursorYear = minYear
    var cursorMonth = minMonth
    while (cursorYear < maxYear || (cursorYear == maxYear && cursorMonth <= maxMonth)) {
      val key = Pair(cursorYear, cursorMonth)
      val monthTxs = monthlyGrouped[key].orEmpty()
      val inflows = monthTxs.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amount }
      val outflows =
        monthTxs.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amount }
      val surplus = inflows - outflows
      runningCash += surplus
      val allocatable = max(0.0, surplus)
      historical.add(
        MonthlyCashSnapshot(
          year = cursorYear,
          month = cursorMonth,
          label = FinanceFormatter.monthShort(cursorYear, cursorMonth),
          fullLabel = FinanceFormatter.monthFull(cursorYear, cursorMonth),
          inflows = inflows,
          outflows = outflows,
          surplus = surplus,
          cumulativeCash = runningCash,
          essential50 = allocatable * 0.50,
          leisure30 = allocatable * 0.30,
          debtsInvest20 = allocatable * 0.20,
          isProjected = false,
        )
      )
      val next = incrementMonth(cursorYear, cursorMonth)
      cursorYear = next.first
      cursorMonth = next.second
    }

    val dailyExpenseAvg = totalExpense / spanDays
    val weeklyExpenseAvg = dailyExpenseAvg * 7.0
    val monthlyExpenseAvg = totalExpense / spanMonths
    val annualExpenseAvg = monthlyExpenseAvg * 12.0
    val monthlyIncomeAvg = totalIncome / spanMonths
    val monthlySurplusAvg = monthlyIncomeAvg - monthlyExpenseAvg

    val averages =
      SpendingAverages(
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        cashBalance = cashBalance,
        dailyExpenseAvg = dailyExpenseAvg,
        weeklyExpenseAvg = weeklyExpenseAvg,
        monthlyExpenseAvg = monthlyExpenseAvg,
        annualExpenseAvg = annualExpenseAvg,
        monthlyIncomeAvg = monthlyIncomeAvg,
        monthlySurplusAvg = monthlySurplusAvg,
        activeDaysSpan = spanDays,
        activeMonthsSpan = spanMonths,
      )

    val (projStartYear, projStartMonth) = incrementMonth(maxYear, maxMonth)
    val projected =
      buildProjectedMonths(
        startYear = projStartYear,
        startMonth = projStartMonth,
        avgInflows = monthlyIncomeAvg,
        avgOutflows = monthlyExpenseAvg,
        startingCash = runningCash,
        count = PROJECTION_MONTHS_COUNT,
      )

    val goalProjections =
      goals.map { goal ->
        val remaining = max(0.0, goal.targetAmount - cashBalance)
        val progress =
          if (goal.targetAmount > 0.0) (cashBalance / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
          else 0f
        val estMonths =
          when {
            remaining <= 0.0 -> 0
            monthlySurplusAvg > 0.0 -> ceil(remaining / monthlySurplusAvg).toInt()
            else -> null
          }
        GoalProjection(
          goal = goal,
          currentCash = cashBalance,
          remainingAmount = remaining,
          progressFraction = progress,
          estimatedMonths = estMonths,
          isAchieved = remaining <= 0.0,
        )
      }

    // Average of the 20% Debts & Investments bucket across all recorded months
    val maxInstallment20 =
      if (historical.isEmpty()) 0.0
      else historical.sumOf { it.debtsInvest20 } / historical.size

    val debtSuggestions =
      debts.map { debt ->
        val rem = debt.remainingAmount
        when {
          rem <= 0.0 ->
            DebtSuggestion(
              debt = debt,
              remainingDebt = 0.0,
              maxInstallment20 = maxInstallment20,
              suggestedMonths = 0,
              suggestedInstallment = 0.0,
              lastInstallment = 0.0,
            )
          maxInstallment20 > 0.0 -> {
            val months = ceil(rem / maxInstallment20).toInt().coerceAtLeast(1)
            val regular = min(rem, maxInstallment20)
            val last = rem - (regular * (months - 1))
            DebtSuggestion(
              debt = debt,
              remainingDebt = rem,
              maxInstallment20 = maxInstallment20,
              suggestedMonths = months,
              suggestedInstallment = regular,
              lastInstallment = if (last > 0.01) last else regular,
            )
          }
          else ->
            DebtSuggestion(
              debt = debt,
              remainingDebt = rem,
              maxInstallment20 = 0.0,
              suggestedMonths = null,
              suggestedInstallment = 0.0,
              lastInstallment = 0.0,
            )
        }
      }

    val totalRemainingDebt = debts.sumOf { it.remainingAmount }
    val combinedDebtPayoffMonths =
      when {
        totalRemainingDebt <= 0.0 -> 0
        maxInstallment20 > 0.0 -> ceil(totalRemainingDebt / maxInstallment20).toInt()
        else -> null
      }

    return SmartSpreadsheetSummary(
      averages = averages,
      historicalMonths = historical,
      projectedMonths = projected,
      allMonthsForChart = historical + projected,
      goalProjections = goalProjections,
      debtSuggestions = debtSuggestions,
      maxMonthlyDebtInstallment20 = maxInstallment20,
      totalRemainingDebt = totalRemainingDebt,
      combinedDebtPayoffMonths = combinedDebtPayoffMonths,
    )
  }

  private fun buildProjectedMonths(
    startYear: Int,
    startMonth: Int,
    avgInflows: Double,
    avgOutflows: Double,
    startingCash: Double,
    count: Int,
  ): List<MonthlyCashSnapshot> {
    val result = mutableListOf<MonthlyCashSnapshot>()
    var y = startYear
    var m = startMonth
    var cash = startingCash
    val surplus = avgInflows - avgOutflows
    val allocatable = max(0.0, surplus)
    repeat(count) {
      cash += surplus
      result.add(
        MonthlyCashSnapshot(
          year = y,
          month = m,
          label = FinanceFormatter.monthShort(y, m),
          fullLabel = FinanceFormatter.monthFull(y, m),
          inflows = avgInflows,
          outflows = avgOutflows,
          surplus = surplus,
          cumulativeCash = cash,
          essential50 = allocatable * 0.50,
          leisure30 = allocatable * 0.30,
          debtsInvest20 = allocatable * 0.20,
          isProjected = true,
        )
      )
      val next = incrementMonth(y, m)
      y = next.first
      m = next.second
    }
    return result
  }

  private fun incrementMonth(year: Int, month1To12: Int): Pair<Int, Int> {
    return if (month1To12 >= 12) Pair(year + 1, 1) else Pair(year, month1To12 + 1)
  }
}
