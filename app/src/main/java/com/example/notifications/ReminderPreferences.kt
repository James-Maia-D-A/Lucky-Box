package com.example.notifications

import android.content.Context

/**
 * Armazena localmente preferências de lembretes diários e um cache leve dos
 * nomes de objetivos e dívidas ativos para personalizar notificações em segundo plano.
 */
object ReminderPreferences {

  private const val PREFS_NAME = "lucky_box_reminders"
  private const val KEY_ENABLED = "reminders_enabled"
  private const val KEY_GOALS = "cached_goal_names"
  private const val KEY_DEBTS = "cached_debt_names"
  private const val KEY_LAST_MSG_IDX = "last_message_index"
  private const val SEPARATOR = "\u001F"

  fun isEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

  fun setEnabled(context: Context, enabled: Boolean) {
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ENABLED, enabled)
      .apply()
  }

  fun syncContext(context: Context, activeGoalNames: List<String>, activeDebtNames: List<String>) {
    val cleanGoals = activeGoalNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(15)
    val cleanDebts = activeDebtNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(15)
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_GOALS, cleanGoals.joinToString(SEPARATOR))
      .putString(KEY_DEBTS, cleanDebts.joinToString(SEPARATOR))
      .apply()
  }

  fun clearContext(context: Context) {
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove(KEY_GOALS)
      .remove(KEY_DEBTS)
      .apply()
  }

  fun getGoalNames(context: Context): List<String> {
    val raw =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_GOALS, "") ?: ""
    return raw.split(SEPARATOR).filter { it.isNotBlank() }
  }

  fun getDebtNames(context: Context): List<String> {
    val raw =
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DEBTS, "") ?: ""
    return raw.split(SEPARATOR).filter { it.isNotBlank() }
  }

  fun getLastMessageIndex(context: Context): Int =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_LAST_MSG_IDX, -1)

  fun setLastMessageIndex(context: Context, index: Int) {
    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putInt(KEY_LAST_MSG_IDX, index)
      .apply()
  }
}
