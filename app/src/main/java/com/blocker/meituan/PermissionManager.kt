package com.blocker.meituan

import rikka.shizuku.Shizuku
import java.io.InputStream

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

    fun getState(): PermState {
        val locGranted = runShell("pm check-permission android.permission.ACCESS_FINE_LOCATION $MEITUAN_PKG")
            .trim() == "PERMISSION_GRANTED"
        val btGranted = runShell("pm check-permission android.permission.BLUETOOTH_CONNECT $MEITUAN_PKG")
            .trim() == "PERMISSION_GRANTED"
        return PermState(locGranted, btGranted)
    }

    fun toggleLocation(grant: Boolean) {
        LOCATION_PERMISSIONS.forEach { perm ->
            val cmd = if (grant) "pm grant $MEITUAN_PKG $perm" else "pm revoke $MEITUAN_PKG $perm"
            runShell(cmd)
        }
    }

    fun toggleBluetooth(grant: Boolean) {
        BLUETOOTH_PERMISSIONS.forEach { perm ->
            val cmd = if (grant) "pm grant $MEITUAN_PKG $perm" else "pm revoke $MEITUAN_PKG $perm"
            runShell(cmd)
        }
    }

    private fun runShell(cmd: String): String {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val output = readStream(process.inputStream)
            process.waitFor()
            output
        } catch (e: Exception) {
            ""
        }
    }

    private fun readStream(stream: InputStream): String {
        return stream.bufferedReader().readText()
    }
}
