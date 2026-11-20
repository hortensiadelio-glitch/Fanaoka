package mg.fanaoka.app.protection

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class FanaokaVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.compareAndSet(false, true)) startTunnel()
        return START_STICKY
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
            worker = thread(name = "Fanaoka-DNS") {
                val input = FileInputStream(fd.fileDescriptor)
                val output = FileOutputStream(fd.fileDescriptor)
                val buffer = ByteArray(32767)
                while (running.get()) {
                    try {
                        val length = input.read(buffer)
                        if (length <= 0) continue
                        val packet = buffer.copyOf(length)
                        val response = DnsPacketProcessor.process(packet, ::protectSocket)
                        if (response != null) output.write(response)
                    } catch (_: Exception) {
                        if (running.get()) continue
                    }
                }
            }
        } catch (_: Exception) {
            running.set(false)
            stopSelf()
        }
    }

    private fun protectSocket(socket: DatagramSocket) = protect(socket)

    override fun onDestroy() {
        running.set(false)
        try { tunnel?.close() } catch (_: Exception) {}
        worker?.interrupt()
        tunnel = null
        super.onDestroy()
    }

    companion object {
        @Volatile var isRunning: Boolean = false
            private set
    }
}
