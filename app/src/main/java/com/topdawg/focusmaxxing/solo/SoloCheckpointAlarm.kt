package com.topdawg.focusmaxxing.solo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object SoloCheckpointAlarm {
    const val ACTION_CHECKPOINT_DUE = "com.topdawg.focusmaxxing.SOLO_CHECKPOINT_DUE"
    const val ACTION_REFRESH_SESSION = "com.topdawg.focusmaxxing.SOLO_REFRESH_SESSION"

    fun schedule(context: Context, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        SoloConstants.NOTIFICATION_ID,
        Intent(context, SoloCheckpointReceiver::class.java).setAction(ACTION_CHECKPOINT_DUE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
