package com.blocker.meituan

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FloatWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var locationBlocked = false
    private var bluetoothBlocked = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        createFloatWindow()
        refreshState()
    }

    private fun startForegroundNotification() {
        val channelId = "blocker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "权限控制", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("美团众包权限控制运行中")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
        startForeground(1, notif)
    }

    private fun createFloatWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatView = LayoutInflater.from(this).inflate(R.layout.float_window, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        var lastX = 0f
        var lastY = 0f
        var isDragging = false

        floatView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        lastX = event.rawX
                        lastY = event.rawY
                        windowManager.updateViewLayout(floatView, params)
                    }
                    true
                }
                else -> isDragging
            }
        }

        floatView.findViewById<View>(R.id.btn_location).setOnClickListener {
            if (!isDragging) toggleLocation()
        }

        floatView.findViewById<View>(R.id.btn_bluetooth).setOnClickListener {
            if (!isDragging) toggleBluetooth()
        }

        floatView.findViewById<View>(R.id.btn_close).setOnClickListener {
            stopSelf()
        }

        windowManager.addView(floatView, params)
    }

    private fun refreshState() {
        scope.launch(Dispatchers.IO) {
            val state = PermissionManager.getState()
            withContext(Dispatchers.Main) {
                locationBlocked = !state.locationGranted
                bluetoothBlocked = !state.bluetoothGranted
                updateUI()
            }
        }
    }

    private fun toggleLocation() {
        scope.launch(Dispatchers.IO) {
            locationBlocked = !locationBlocked
            PermissionManager.toggleLocation(!locationBlocked)
            withContext(Dispatchers.Main) {
                updateUI()
                val msg = if (locationBlocked) "✅ GPS定位已禁用" else "🔓 GPS定位已恢复"
                Toast.makeText(this@FloatWindowService, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleBluetooth() {
        scope.launch(Dispatchers.IO) {
            bluetoothBlocked = !bluetoothBlocked
            PermissionManager.toggleBluetooth(!bluetoothBlocked)
            withContext(Dispatchers.Main) {
                updateUI()
                val msg = if (bluetoothBlocked) "✅ 蓝牙已禁用" else "🔓 蓝牙已恢复"
                Toast.makeText(this@FloatWindowService, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        floatView.findViewById<TextView>(R.id.tv_location).text =
            if (locationBlocked) "GPS\n已禁" else "GPS\n正常"
        floatView.findViewById<View>(R.id.btn_location).setBackgroundResource(
            if (locationBlocked) R.drawable.btn_blocked else R.drawable.btn_normal
        )
        floatView.findViewById<TextView>(R.id.tv_bluetooth).text =
            if (bluetoothBlocked) "蓝牙\n已禁" else "蓝牙\n正常"
        floatView.findViewById<View>(R.id.btn_bluetooth).setBackgroundResource(
            if (bluetoothBlocked) R.drawable.btn_blocked else R.drawable.btn_normal
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::floatView.isInitialized) {
            windowManager.removeView(floatView)
        }
    }
}
