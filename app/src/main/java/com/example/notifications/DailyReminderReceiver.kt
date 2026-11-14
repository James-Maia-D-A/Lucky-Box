package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Recebe os alarmes diários (3x por dia) e o evento de boot do dispositivo para
 * exibir lembretes personalizados acompanhados da imagem transparente do Leprechaun.
 */
class DailyReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED -> {
        DailyReminderScheduler.scheduleAll(context)
      }
      DailyReminderScheduler.ACTION_SEND_REMINDER -> {
        val slotCode = intent.getIntExtra(DailyReminderScheduler.EXTRA_SLOT_CODE, -1)
        if (slotCode != -1) {
          DailyReminderScheduler.rescheduleSlotForTomorrow(context, slotCode)
        }
        if (ReminderPreferences.isEnabled(context)) {
          showReminderNotification(context, notificationId = if (slotCode != -1) slotCode else 1099)
        }
      }
    }
  }

  companion object {
    const val CHANNEL_ID = "lucky_box_daily_reminders"

    fun showReminderNotification(context: Context, notificationId: Int = 1099): Boolean {
      if (!canPostNotifications(context)) return false

      ensureChannel(context)

      val goals = ReminderPreferences.getGoalNames(context)
      val debts = ReminderPreferences.getDebtNames(context)
      val lastIdx = ReminderPreferences.getLastMessageIndex(context)
      val rendered = ReminderMessageCatalog.pickMessage(goals, debts, lastIdx)
      ReminderPreferences.setLastMessageIndex(context, rendered.index)

      val openAppIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      val contentPendingIntent =
        PendingIntent.getActivity(
          context,
          notificationId,
          openAppIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      val portraitBitmap = decodeTransparentPortrait(context)

      val builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_notification_clover)
          .setContentTitle(rendered.title)
          .setContentText(rendered.body)
          .setStyle(
            NotificationCompat.BigTextStyle()
              .setBigContentTitle(rendered.title)
              .bigText(rendered.body)
          )
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setAutoCancel(true)
          .setContentIntent(contentPendingIntent)

      if (portraitBitmap != null) {
        builder.setLargeIcon(portraitBitmap)
      }

      NotificationManagerCompat.from(context).notify(notificationId, builder.build())
      return true
    }

    fun canPostNotifications(context: Context): Boolean {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted =
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return false
      }
      return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager =
          context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
          val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
              )
              .apply {
                description = context.getString(R.string.notification_channel_desc)
              }
          manager.createNotificationChannel(channel)
        }
      }
    }

    private fun decodeTransparentPortrait(context: Context): Bitmap? =
      try {
        BitmapFactory.decodeResource(context.resources, R.drawable.img_leprechaun_notification)
      } catch (_: Exception) {
        null
      }
  }
}
