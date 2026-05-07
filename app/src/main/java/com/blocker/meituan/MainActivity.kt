package com.blocker.meituan

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkShizuku()) return

        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("请在下一个页面中开启「显示在其他应用上层」权限")
                .setPositiveButton("去开启") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                }
                .setNegativeButton("取消") { _, _ -> finish() }
                .show()
            return
        }

        startFloatService()
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startFloatService()
            finish()
        }
    }

    private fun checkShizuku(): Boolean {
        try {
            if (!Shizuku.pingBinder()) {
                AlertDialog.Builder(this)
                    .setTitle("Shizuku 未运行")
                    .setMessage("请先安装并启动 Shizuku，然后重新打开本应用。\n\n下载地址：https://github.com/RikkaApps/Shizuku/releases")
                    .setPositiveButton("确定") { _, _ -> finish() }
                    .show()
                return false
            }
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1001)
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "请先安装 Shizuku", Toast.LENGTH_LONG).show()
            finish()
            return false
        }
        return true
    }

    private fun startFloatService() {
        startService(Intent(this, FloatWindowService::class.java))
        Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show()
    }
}
