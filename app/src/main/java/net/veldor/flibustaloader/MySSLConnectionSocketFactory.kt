package net.veldor.flibustaloader

import kotlin.Throws
import cz.msebera.android.httpclient.HttpHost
import cz.msebera.android.httpclient.conn.ssl.SSLConnectionSocketFactory
import cz.msebera.android.httpclient.protocol.HttpContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import javax.net.ssl.SSLContext

class MySSLConnectionSocketFactory(sslContext: SSLContext?) :
    SSLConnectionSocketFactory(sslContext) {
    override fun createSocket(context: HttpContext): Socket {
        val socksaddr = context.getAttribute("socks.address") as InetSocketAddress
        val proxy = Proxy(Proxy.Type.SOCKS, socksaddr)
        return Socket(proxy)
    }

    @Throws(IOException::class)
    override fun connectSocket(
        connectTimeout: Int, socket: Socket, host: HttpHost, remoteAddress: InetSocketAddress,
        localAddress: InetSocketAddress, context: HttpContext
    ): Socket {
        val unresolvedRemote = InetSocketAddress
            .createUnresolved(host.hostName, remoteAddress.port)
        return super.connectSocket(
            connectTimeout,
            socket,
            host,
            unresolvedRemote,
            localAddress,
            context
        )
    }
}