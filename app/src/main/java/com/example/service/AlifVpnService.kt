package com.example.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

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
        } catch (e: Exception) {
            Log.e("AlifVpnService", "Failed to establish VPN interface: ${e.message}")
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
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
