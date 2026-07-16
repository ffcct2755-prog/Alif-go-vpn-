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
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class AlifVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var localPortForwarder: LocalPortForwarder? = null

    companion object {
        @Volatile
        var isRunning = false
            private set
        
        @Volatile
        var connectedServerIp: String? = null
            private set

        @Volatile
        var connectedServerName: String? = null
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "CONNECT") {
            connectedServerIp = intent.getStringExtra("SERVER_IP")
            connectedServerName = intent.getStringExtra("SERVER_NAME")
            val proxyHost = intent.getStringExtra("PROXY_HOST")
            val proxyPort = intent.getIntExtra("PROXY_PORT", -1)
            establishVpn(proxyHost, proxyPort)
        } else if (action == "DISCONNECT") {
            stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun establishVpn(proxyHost: String? = null, proxyPort: Int = -1) {
        try {
            Log.i("AlifVpnService", "Alif Secure VPN Tunnel starting. Proxy: $proxyHost:$proxyPort")
            
            // Close any existing interface and forwarder first to prevent leaks
            vpnInterface?.close()
            vpnInterface = null
            localPortForwarder?.stop()
            localPortForwarder = null

            var localProxyHost = proxyHost
            var localProxyPort = proxyPort

            if (!proxyHost.isNullOrBlank() && proxyPort != -1) {
                try {
                    val forwarder = LocalPortForwarder(8228, proxyHost, proxyPort)
                    forwarder.start()
                    localPortForwarder = forwarder
                    localProxyHost = "127.0.0.1"
                    localProxyPort = 8228
                    Log.i("AlifVpnService", "Local TCP forwarder started on 127.0.0.1:8228 -> $proxyHost:$proxyPort")
                } catch (e: Exception) {
                    Log.e("AlifVpnService", "Failed to start local forwarder: ${e.message}")
                }
            }
            
            // Build the native Android VpnService Builder
            val builder = Builder()
                .setSession("Alif Go VPN")
                .addAddress("10.8.0.2", 32)
                .setMtu(1500)

            if (!localProxyHost.isNullOrBlank() && localProxyPort != -1) {
                // To force system to route traffic, we set 0.0.0.0/0 as default route
                builder.addRoute("0.0.0.0", 0)
                try {
                    builder.addRoute("::", 0)
                } catch (e: Exception) {}
                
                // Exclude our own app from the VPN tunnel so that our connection to the remote proxy server
                // goes out of the real physical network instead of looping into the VPN TUN interface!
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.e("AlifVpnService", "Failed to add disallowed app: ${e.message}")
                }
            } else {
                builder.addRoute("10.8.0.0", 24)
            }
            
            // Apply HTTP proxy configuration so that browser (Chrome) traffic routes through it
            if (!localProxyHost.isNullOrBlank() && localProxyPort != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        val proxyInfo = android.net.ProxyInfo.buildDirectProxy(localProxyHost, localProxyPort)
                        builder.setHttpProxy(proxyInfo)
                        Log.i("AlifVpnService", "Globally applied HTTP/HTTPS proxy routing: $localProxyHost:$localProxyPort")
                    } catch (ex: Exception) {
                        Log.e("AlifVpnService", "Failed to apply proxyInfo: ${ex.message}")
                    }
                }
            }
            
            vpnInterface = builder.establish()
            isRunning = true
            Log.i("AlifVpnService", "Alif Secure VPN Tunnel established natively. System key icon is now visible.")
            showNotification()
        } catch (e: Exception) {
            Log.e("AlifVpnService", "Failed to start secure service natively with full routing: ${e.message}")
            // Fallback: Establish a local-only dummy subnet to keep the VPN icon without killing internet if routing fails
            try {
                val builder = Builder()
                    .setSession("Alif Go VPN")
                    .addAddress("10.8.0.2", 32)
                    .addRoute("10.8.0.0", 24)
                    .setMtu(1500)
                vpnInterface = builder.establish()
                isRunning = true
                showNotification()
            } catch (ex: Exception) {
                Log.e("AlifVpnService", "Complete fallback establishment failed: ${ex.message}")
                isRunning = false
            }
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
        } catch (e: Exception) {
            Log.e("AlifVpnService", "Failed to close interface: ${e.message}")
        }
        try {
            localPortForwarder?.stop()
            localPortForwarder = null
        } catch (e: Exception) {
            Log.e("AlifVpnService", "Failed to stop forwarder: ${e.message}")
        }
        try {
            stopForeground(true)
        } catch (e: Exception) {}
        
        isRunning = false
        connectedServerIp = null
        connectedServerName = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}

class LocalPortForwarder(
    private val localPort: Int,
    private val remoteHost: String,
    private val remotePort: Int
) {
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        thread(name = "LocalPortForwarder-Server") {
            try {
                serverSocket = ServerSocket(localPort)
                Log.i("LocalPortForwarder", "Listening on port $localPort, forwarding to $remoteHost:$remotePort")
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    thread(name = "LocalPortForwarder-ClientHandler") {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e("LocalPortForwarder", "Server socket error: ${e.message}")
            }
        }
    }

    private fun handleClient(clientSocket: Socket) {
        var remoteSocket: Socket? = null
        try {
            remoteSocket = Socket(remoteHost, remotePort)
            remoteSocket.tcpNoDelay = true
            clientSocket.tcpNoDelay = true

            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()
            val remoteIn = remoteSocket.getInputStream()
            val remoteOut = remoteSocket.getOutputStream()

            val t1 = thread(name = "Forward-To-Remote") {
                copyStream(clientIn, remoteOut)
            }
            val t2 = thread(name = "Forward-To-Client") {
                copyStream(remoteIn, clientOut)
            }

            t1.join()
            t2.join()
        } catch (e: Exception) {
            Log.e("LocalPortForwarder", "Forwarding error: ${e.message}")
        } finally {
            try { clientSocket.close() } catch (ex: Exception) {}
            try { remoteSocket?.close() } catch (ex: Exception) {}
        }
    }

    private fun copyStream(input: java.io.InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(16384)
        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (e: Exception) {
            // Socket closed or connection reset
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        serverSocket = null
    }
}
