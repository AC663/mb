package com.blocker.meituan

import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

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
            runShell(if (grant) "pm grant $MEITUAN_PKG $perm" else "pm revoke $MEITUAN_PKG $perm")
        }
    }

    fun toggleBluetooth(grant: Boolean) {
        BLUETOOTH_PERMISSIONS.forEach { perm ->
            runShell(if (grant) "pm grant $MEITUAN_PKG $perm" else "pm revoke $MEITUAN_PKG $perm")
        }
    }

    private fun runShell(cmd: String): String {
        return try {
            val process: ShizukuRemoteProcess = Shizuku::class.java
                .getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .invoke(null, arrayOf("sh", "-c", cmd), null, null) as ShizukuRemoteProcess
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output
            } catch (e2: Exception) {
                ""
            }
        }
    }
}
