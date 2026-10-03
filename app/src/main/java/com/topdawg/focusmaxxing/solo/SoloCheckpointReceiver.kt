package com.topdawg.focusmaxxing.solo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Notification delivery is wired with checkpoint flows; kept as a receiver entry point for alarms. */
class SoloCheckpointReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Stage 4 adds notification routing to the active checkpoint UI.
    }
}
