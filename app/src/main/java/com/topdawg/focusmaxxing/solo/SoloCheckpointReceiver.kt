package com.topdawg.focusmaxxing.solo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.topdawg.focusmaxxing.MainActivity

/** Notification delivery is wired with checkpoint flows; kept as a receiver entry point for alarms. */
class SoloCheckpointReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SoloCheckpointAlarm.ACTION_CHECKPOINT_DUE) return
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        if (processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) return
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(SoloConstants.NOTIFICATION_CHANNEL_ID, "Study checkpoints", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_CHECKPOINT, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = android.app.PendingIntent.getActivity(context, SoloConstants.NOTIFICATION_ID, open, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, SoloConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Study checkpoint")
            .setContentText("Pause and record what you have covered.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(SoloConstants.NOTIFICATION_ID, notification) }
    }
}
