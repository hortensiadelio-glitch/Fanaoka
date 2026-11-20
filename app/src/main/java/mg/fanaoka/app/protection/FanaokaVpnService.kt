package mg.fanaoka.app.protection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class FanaokaVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.compareAndSet(false, true)) {
            startAsForeground()
            startTunnel()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "fanaoka_protection"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Protection Fanaoka", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("Fanaoka protège vos requêtes DNS")
            .setContentText("Filtrage DNS local actif")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        startForeground(4102, notification)
    }

    private fun startTunnel() {
        try {
            tunnel = Builder()
                .setSession("Fanaoka DNS")
                .setMtu(1500)
                .addAddress("10.10.0.1", 32)
                .addDnsServer("10.10.0.2")
                .addRoute("10.10.0.2", 32)
                .establish()
            val fd = tunnel ?: throw IllegalStateException("Tunnel VPN indisponible")
            isRunning = true
            worker = thread(name = "Fanaoka-DNS") {
                val input = FileInputStream(fd.fileDescriptor)
                val output = FileOutputStream(fd.fileDescriptor)
                val buffer = ByteArray(32767)
                while (running.get()) {
                    try {
                        val length = input.read(buffer)
                        if (length <= 0) continue
                        val response = DnsPacketProcessor.process(buffer.copyOf(length), ::protectSocket)
                        if (response != null) output.write(response)
                    } catch (_: Exception) {
                        if (running.get()) continue
                    }
                }
            }
        } catch (_: Exception) {
            isRunning = false
            running.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun protectSocket(socket: DatagramSocket): Boolean = protect(socket)

    override fun onRevoke() {
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        running.set(false)
        isRunning = false
        try { tunnel?.close() } catch (_: Exception) {}
        worker?.interrupt()
        tunnel = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        @Volatile var isRunning: Boolean = false
            private set
    }
}
