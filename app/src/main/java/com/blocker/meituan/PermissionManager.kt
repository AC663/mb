package com.blocker.meituan

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import rikka.shizuku.Shizuku

object PermissionManager {

    private const val MEITUAN_PKG = "com.sankuai.meituan.dispatch.crowdsource"

    private val LOCATION_PERMISSIONS = listOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )

    private val BLUETOOTH_PERMISSIONS = listOf(
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.BLUETOOTH"
    )

    data class PermState(val locationGranted: Boolean, val bluetoothGranted: Boolean)

    private var shellService: IShellService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.blocker.meituan", ShellService::class.java.name)
    ).daemon(false).processNameSuffix("shell_service").debuggable(false).version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            shellService = IShellService::class.java.cast(binder)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
        }
    }

    fun bindService() {
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unbindService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getState(): PermState {
        val locGranted = runShell("pm check-permission android.permission.ACCESS_FINE_LOCATION $MEITUAN_PKG")
            .trim() == "PERMISSION_GRANTED"
        val btGranted = runShell("pm check-permission android.permission.BLUETOOTH_CONNECT $MEITUAN_PKG")
            .trim() == "PERMISSION_GRANTED"
        return PermState(locGranted, btGranted)
    }

    fun toggleLocation(grant: Boolean) {
        LOCATION_PERMISSIONS.forEach { perm ->
            runShell(if (grant) "pm grant $MEITUAN_PKG $perm" else "pm revoke $MEITUAN_PKG $perm")
        }
    }

    fun toggleBluetooth(grant: Boolean) {
        BLUETOOTH_PERMISSIONS.forEach { perm ->
            runShell(if (grant) "pm grant $MEITUAN_PKG $perm" else "pm revoke $MEITUAN_PKG $perm")
        }
    }

    private fun runShell(cmd: String): String {
        return shellService?.runCommand(cmd) ?: ""
    }
}
