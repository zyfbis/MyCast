package com.example.mycast.Tunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

public class MySocket {
    private Socket mSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public MySocket(Socket socket) throws IOException {
        mSocket = socket;
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
    }

    public boolean isConnected() {
        return mSocket.isConnected();
    }

    public boolean send(DatagramPacket packet) throws IOException {
        if (!packet.getAddress().equals(mSocket.getInetAddress())) {
            return false;
        }
        if (packet.getPort() <= 0 || packet.getPort() >= 65536) {
            return false;
        }

        int port = packet.getPort();
        int length = packet.getLength();
        byte[] bPort = i2s(port);
        byte[] bLength = i2s(length);
        byte[] buf = packet.getData();
        int offset = packet.getOffset();
        byte[] data = new byte[4 + 4 + length];
        System.arraycopy(bPort, 0, data, 0, 4);
        System.arraycopy(bLength, 0, data, 4, 4);
        System.arraycopy(buf, offset, data, 8, length);
        outputStream.write(data);
        return true;
    }

    public DatagramPacket recv() {
        DatagramPacket packet = null;
        try {
            int port = readInt();
            int length = readInt();
            if (length < 0 || port < 0 || port > 65535) {
                mSocket.close();
                return null;
            }

            byte[] buf = new byte[length];

            int read = 0;
            while (true) {
                read += inputStream.read(buf, read, length - read);
                if (read == length) {
                    break;
                } else {
                    Thread.sleep(1);
                }
            }

            packet = new DatagramPacket(buf, 0, length, InetAddress.getLocalHost(), port);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return packet;
    }

    private static byte[] i2s(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }

    private static int s2i(byte[] b) {
        int res = 0;
        for (int i = 0; i < b.length; i++) {
            res += (b[i] & 0xff) << (i * 8);
        }
        return res;
    }

    private void writeInt(int val) throws IOException {
        byte[] bVal = i2s(val);
        outputStream.write(bVal, 0, 4);
    }

    private int readInt() throws IOException, InterruptedException {
        byte[] bVal = new byte[4];

        int read = 0;
        while (true) {
            read += inputStream.read(bVal, read, 4 - read);
            if (read == 4) {
                break;
            } else {
                Thread.sleep(1);
            }
        }

        return s2i(bVal);
    }

    public Socket getSocket() {
        return mSocket;
    }
}
