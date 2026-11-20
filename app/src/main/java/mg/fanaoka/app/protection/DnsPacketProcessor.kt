package mg.fanaoka.app.protection

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

internal object DnsPacketProcessor {
    private const val DNS_IP = "10.10.0.2"
    private const val UPSTREAM = "1.1.1.1"
    private val blocked = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "ads.facebook.com", "adnxs.com",
        "adform.net", "taboola.com", "outbrain.com"
    )

    fun process(packet: ByteArray, protect: (DatagramSocket) -> Boolean): ByteArray? {
        if (packet.size < 28 || (packet[0].toInt() ushr 4 and 0x0f) != 4) return null
        val ihl = (packet[0].toInt() and 0x0f) * 4
        if (ihl < 20 || packet.size < ihl + 8 + 12) return null
        if ((packet[9].toInt() and 0xff) != 17) return null
        val udpLength = u16(packet, ihl + 4)
        if (udpLength < 20 || packet.size < ihl + udpLength) return null
        val sourcePort = u16(packet, ihl)
        val destPort = u16(packet, ihl + 2)
        if (destPort != 53) return null
        val dnsStart = ihl + 8
        val dns = packet.copyOfRange(dnsStart, ihl + udpLength)
        val domain = readName(dns) ?: return null
        val responseDns = if (isBlocked(domain)) blockedResponse(dns) else forward(dns, protect) ?: return null
        return buildIpv4UdpResponse(packet, ihl, sourcePort, responseDns)
    }

    private fun readName(dns: ByteArray): String? {
        if (dns.size < 17) return null
        var i = 12
        val labels = mutableListOf<String>()
        while (i < dns.size) {
            val size = dns[i++].toInt() and 0xff
            if (size == 0) break
            if ((size and 0xc0) != 0 || size > 63 || i + size > dns.size) return null
            labels += String(dns, i, size, Charsets.US_ASCII).lowercase()
            i += size
        }
        return labels.takeIf { it.isNotEmpty() }?.joinToString(".")
    }

    private fun isBlocked(domain: String): Boolean =
        blocked.any { domain == it || domain.endsWith(".$it") }

    private fun blockedResponse(query: ByteArray): ByteArray {
        val end = questionEnd(query)
        val qd = query.copyOfRange(12, end)
        val response = ByteArray(12 + qd.size)
        response[0] = query[0]; response[1] = query[1]
        response[2] = 0x81.toByte(); response[3] = 0x83.toByte()
        response[4] = query[4]; response[5] = query[5]
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0; response[10] = 0; response[11] = 0
        System.arraycopy(qd, 0, response, 12, qd.size)
        return response
    }

    private fun questionEnd(dns: ByteArray): Int {
        var i = 12
        while (i < dns.size) {
            val n = dns[i++].toInt() and 0xff
            if (n == 0) return (i + 4).coerceAtMost(dns.size)
            if ((n and 0xc0) != 0 || n > 63 || i + n > dns.size) return dns.size
            i += n
        }
        return dns.size
    }

    private fun forward(dns: ByteArray, protect: (DatagramSocket) -> Boolean): ByteArray? {
        val socket = DatagramSocket()
        return try {
            if (!protect(socket)) return null
            socket.soTimeout = 2500
            val address = InetAddress.getByName(UPSTREAM)
            socket.send(DatagramPacket(dns, dns.size, address, 53))
            val buffer = ByteArray(4096)
            val reply = DatagramPacket(buffer, buffer.size)
            socket.receive(reply)
            buffer.copyOf(reply.length)
        } catch (_: Exception) {
            null
        } finally {
            socket.close()
        }
    }

    private fun buildIpv4UdpResponse(request: ByteArray, ipHeaderLength: Int, clientPort: Int, dns: ByteArray): ByteArray {
        val udpLength = 8 + dns.size
        val totalLength = ipHeaderLength + udpLength
        val out = ByteArray(totalLength)
        System.arraycopy(request, 0, out, 0, ipHeaderLength)
        for (i in 0..3) {
            out[12 + i] = request[16 + i]
            out[16 + i] = request[12 + i]
        }
        out[2] = (totalLength ushr 8).toByte(); out[3] = totalLength.toByte()
        out[8] = 64
        out[10] = 0; out[11] = 0
        val checksum = ipChecksum(out, ipHeaderLength)
        out[10] = (checksum ushr 8).toByte(); out[11] = checksum.toByte()
        out[ipHeaderLength] = 0x00; out[ipHeaderLength + 1] = 53
        out[ipHeaderLength + 2] = (clientPort ushr 8).toByte()
        out[ipHeaderLength + 3] = clientPort.toByte()
        out[ipHeaderLength + 4] = (udpLength ushr 8).toByte()
        out[ipHeaderLength + 5] = udpLength.toByte()
        out[ipHeaderLength + 6] = 0; out[ipHeaderLength + 7] = 0
        System.arraycopy(dns, 0, out, ipHeaderLength + 8, dns.size)
        return out
    }

    private fun ipChecksum(data: ByteArray, length: Int): Int {
        var sum = 0L
        var i = 0
        while (i < length) {
            sum += (((data[i].toInt() and 0xff) shl 8) or (data[i + 1].toInt() and 0xff)).toLong()
            i += 2
        }
        while (sum shr 16 != 0L) sum = (sum and 0xffff) + (sum shr 16)
        return sum.inv().toInt() and 0xffff
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
}
