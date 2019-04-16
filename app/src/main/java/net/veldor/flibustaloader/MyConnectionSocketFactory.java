package net.veldor.flibustaloader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.protocol.HttpContext;

/**
 * Created by Jehy on 14.10.2016.
 */


class MyConnectionSocketFactory implements ConnectionSocketFactory {

    @Override
    public Socket createSocket(final HttpContext context) {
        //InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
        //Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
        return new Socket();//(proxy);
    }

    @Override
    public Socket connectSocket(
            int connectTimeout,
            Socket socket,
            final HttpHost host,
            final InetSocketAddress remoteAddress,
            final InetSocketAddress localAddress,
            final HttpContext context) throws IOException {

        InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
        socket = new Socket();
        connectTimeout = 100000;
        socket.setSoTimeout(connectTimeout);
        socket.connect(new InetSocketAddress(socksaddr.getHostName(), socksaddr.getPort()), connectTimeout);


        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.write((byte) 0x04);
        outputStream.write((byte) 0x01);
        outputStream.writeShort((short) host.getPort());
        outputStream.writeInt(0x01);
        outputStream.write((byte) 0x00);
        outputStream.write(host.getHostName().getBytes());
        outputStream.write((byte) 0x00);

        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        if (inputStream.readByte() != (byte) 0x00 || inputStream.readByte() != (byte) 0x5a) {
            throw new IOException("SOCKS4a connect failed");
        }
        inputStream.readShort();
        inputStream.readInt();
        return socket;
    }
}