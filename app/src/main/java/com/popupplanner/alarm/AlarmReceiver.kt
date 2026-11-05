package com.popupplanner.alarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.popupplanner.ui.ReminderActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_PLAN) return

        val planId = intent.getLongExtra(EXTRA_PLAN_ID, -1L)
        if (planId <= 0L) return

        val planTitle = intent.getStringExtra(EXTRA_PLAN_TITLE)
            ?.takeIf { it.isNotBlank() }

        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        val reminderIntent = Intent(appContext, ReminderActivity::class.java).apply {
            action = ACTION_SHOW_REMINDER
            data = Uri.parse("popupplanner://reminder/$planId")
            putExtra(EXTRA_PLAN_ID, planId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            notificationId(planId),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The high-priority notification is the OEM-safe fallback for background
        // activity-start restrictions. Posting it first also means ReminderActivity
        // can reliably cancel it when it opens.
        postNotification(appContext, planId, planTitle, contentIntent)

        try {
            appContext.startActivity(reminderIntent)
        } catch (throwable: Throwable) {
            // The notification and its fullScreenIntent remain as the fallback.
            Log.e(TAG, "Could not start ReminderActivity directly", throwable)
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        context: Context,
        planId: Long,
        planTitle: String?,
        contentIntent: PendingIntent
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!areReminderNotificationsEnabled(context)) {
            Log.w(TAG, "Notifications are disabled; relying on direct activity start")
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(planTitle ?: "计划时间到了")
            .setContentText("deadline · 点按查看提醒")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        try {
            manager.notify(notificationId(planId), notification)
        } catch (securityException: SecurityException) {
            Log.w(TAG, "Notification permission is missing", securityException)
        }
    }

    companion object {
        const val ACTION_FIRE_PLAN = "com.popupplanner.action.FIRE_PLAN"
        const val EXTRA_PLAN_ID = "com.popupplanner.extra.PLAN_ID"
        const val EXTRA_PLAN_TITLE = "com.popupplanner.extra.PLAN_TITLE"

        private const val ACTION_SHOW_REMINDER = "com.popupplanner.action.SHOW_REMINDER"
        private const val CHANNEL_ID = "deadline_reminders"
        private const val TAG = "AlarmReceiver"

        /** Stable notification id for a plan, used for posting and cancelling. */
        fun notificationId(planId: Long): Int {
            return (planId xor (planId ushr 32)).toInt()
        }

        fun ensureNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "到点提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "计划到达指定时间时显示全屏提醒"
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        /** App-level notifications plus the reminder channel must both be enabled. */
        fun areReminderNotificationsEnabled(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return false
            }
            val channel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)
            return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        }

        fun cancelNotification(context: Context, planId: Long) {
            NotificationManagerCompat.from(context).cancel(notificationId(planId))
        }
    }
}
