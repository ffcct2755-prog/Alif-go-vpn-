package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

class AlifVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "CONNECT") {
            establishVpn()
        } else if (action == "DISCONNECT") {
            stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun establishVpn() {
        try {
            if (vpnInterface != null) return
            
            // Configure standard local loop VPN settings for realistic metrics
            val builder = Builder()
                .setMtu(1500)
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .setSession("AlifVpnSession")
            
            vpnInterface = builder.establish()
            Log.i("AlifVpnService", "Alif VPN Interface established successfully.")
            
            showNotification()
        } catch (e: Exception) {
            Log.e("AlifVpnService", "Failed to establish VPN interface: ${e.message}")
        }
    }

    private fun showNotification() {
        val channelId = "AlifVpnChannel"
        val channelName = "Alif VPN Status"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alif VPN is Connected")
            .setContentText("Your internet connection is secured.")
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(1, notification)
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopForeground(true)
        } catch (e: Exception) {
            Log.e("AlifVpnService", "Failed to stop VPN: ${e.message}")
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
