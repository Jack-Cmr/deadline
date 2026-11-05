package com.popupplanner.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.popupplanner.data.Plan

enum class ScheduleResult {
    Scheduled,
    ExactAlarmPermissionRequired,
    InThePast,
    Failed
}

/**
 * All exact-alarm handling lives here.
 *
 * There is deliberately no inexact fallback: the product only works if the plan
 * fires at the chosen minute, so a missing permission is reported to the UI.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
    }

    fun schedule(plan: Plan): ScheduleResult {
        if (!canScheduleExactAlarms()) {
            return ScheduleResult.ExactAlarmPermissionRequired
        }
        if (plan.triggerAt <= System.currentTimeMillis()) {
            return ScheduleResult.InThePast
        }

        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                plan.triggerAt,
                alarmPendingIntent(plan)
            )
            ScheduleResult.Scheduled
        } catch (securityException: SecurityException) {
            Log.w(TAG, "Exact alarm permission was revoked while scheduling", securityException)
            ScheduleResult.ExactAlarmPermissionRequired
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not schedule plan ${plan.id}", throwable)
            ScheduleResult.Failed
        }
    }

    fun cancel(plan: Plan) {
        val pendingIntent = alarmPendingIntent(plan)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Opens the per-app "Alarms & reminders" screen. Some OEMs do not expose the
     * Android 12 action, so fall back to the application details page.
     */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val requestIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(requestIntent)
        } catch (notFound: ActivityNotFoundException) {
            Log.w(TAG, "Exact-alarm settings screen unavailable", notFound)
            openAppDetails()
        } catch (throwable: Throwable) {
            Log.w(TAG, "Could not open exact-alarm settings", throwable)
            openAppDetails()
        }
    }

    private fun openAppDetails() {
        val detailsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(detailsIntent)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not open application settings", throwable)
        }
    }

    private fun alarmPendingIntent(plan: Plan): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_PLAN
            data = Uri.parse("popupplanner://plan/${plan.id}")
            putExtra(AlarmReceiver.EXTRA_PLAN_ID, plan.id)
            putExtra(AlarmReceiver.EXTRA_PLAN_TITLE, plan.title)
        }
        return PendingIntent.getBroadcast(
            context,
            plan.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val TAG = "AlarmScheduler"
    }
}
