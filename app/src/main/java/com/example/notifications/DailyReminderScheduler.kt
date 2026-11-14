package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Agenda 3 lembretes diários (manhã 09:00, tarde 14:30 e noite 20:00) usando
 * AlarmManager.setAndAllowWhileIdle sem exigir permissões restritas de alarme exato.
 */
object DailyReminderScheduler {

  data class ReminderSlot(val requestCode: Int, val hour: Int, val minute: Int, val label: String)

  val dailySlots: List<ReminderSlot> =
    listOf(
      ReminderSlot(requestCode = 1001, hour = 9, minute = 0, label = "09:00"),
      ReminderSlot(requestCode = 1002, hour = 14, minute = 30, label = "14:30"),
      ReminderSlot(requestCode = 1003, hour = 20, minute = 0, label = "20:00"),
    )

  const val ACTION_SEND_REMINDER = "com.example.luckybox.ACTION_SEND_DAILY_REMINDER"
  const val EXTRA_SLOT_CODE = "extra_slot_code"

  fun scheduleAll(context: Context) {
    if (!ReminderPreferences.isEnabled(context)) {
      cancelAll(context)
      return
    }
    dailySlots.forEach { slot -> scheduleSlot(context, slot, forceNextDay = false) }
  }

  fun rescheduleSlotForTomorrow(context: Context, slotCode: Int) {
    if (!ReminderPreferences.isEnabled(context)) return
    val slot = dailySlots.firstOrNull { it.requestCode == slotCode } ?: return
    scheduleSlot(context, slot, forceNextDay = true)
  }

  fun cancelAll(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    dailySlots.forEach { slot ->
      val pi = buildPendingIntent(context, slot.requestCode)
      alarmManager.cancel(pi)
      pi.cancel()
    }
  }

  private fun scheduleSlot(context: Context, slot: ReminderSlot, forceNextDay: Boolean) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val now = Calendar.getInstance()
    val target =
      Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, slot.hour)
        set(Calendar.MINUTE, slot.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (forceNextDay || timeInMillis <= now.timeInMillis + 5_000L) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }
    val pi = buildPendingIntent(context, slot.requestCode)
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
  }

  private fun buildPendingIntent(context: Context, requestCode: Int): PendingIntent {
    val intent =
      Intent(context, DailyReminderReceiver::class.java).apply {
        action = ACTION_SEND_REMINDER
        putExtra(EXTRA_SLOT_CODE, requestCode)
      }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
