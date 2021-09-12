package net.veldor.flibustaloader

import android.util.Log
import cz.msebera.android.httpclient.HttpHost
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory
import cz.msebera.android.httpclient.protocol.HttpContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


class MyConnectionSocketFactory : ConnectionSocketFactory {
    override fun createSocket(context: HttpContext): Socket {
        return Socket() //(proxy);
    }

    @Throws(IOException::class)
    override fun connectSocket(
        incomingConnectionTimeout: Int,
        incomingSocket: Socket,
        host: HttpHost,
        remoteAddress: InetSocketAddress?,
        localAddress: InetSocketAddress?,
        context: HttpContext
    ): Socket {
        Log.d("surprise", "connectSocket: create connection to ${host.hostName}")
        val socksaddr = context.getAttribute("socks.address") as InetSocketAddress
        val socket = Socket()
        val connectTimeout = 100000
        socket.soTimeout = connectTimeout
        socket.connect(InetSocketAddress(socksaddr.hostName, socksaddr.port), connectTimeout)
        val outputStream = DataOutputStream(socket.getOutputStream())

        outputStream.write(0x04)
        outputStream.write(0x01)
        outputStream.writeShort(host.port)
        outputStream.writeInt(0x01)
        outputStream.write(0x00)
        outputStream.write(host.hostName.toByteArray())
        outputStream.write(0x00)
        val inputStream = DataInputStream(socket.getInputStream())
        if (inputStream.readByte() != 0x00.toByte() || inputStream.readByte() != 0x5a.toByte()) {
            throw IOException("SOCKS4a connect failed")
        }
        inputStream.readShort()
        inputStream.readInt()
        return socket
    }
}