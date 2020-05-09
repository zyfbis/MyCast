package com.example.mycast.Tunnel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TunnelServer extends Thread {
    public static int DEFAULT_PORT = 7777;
    private int mTcpPort;
    private ServerSocket mServer;
    private ArrayList<MySocket> sockets;

    public TunnelServer() {
        this(DEFAULT_PORT);
    }

    public TunnelServer(int port) {
        mTcpPort = port;
        sockets = new ArrayList<>();
    }

    public void run() {
        try {
            mServer = new ServerSocket(mTcpPort);
            while (!isInterrupted()) {
                Socket socket = mServer.accept();
                sockets.add(new MySocket(socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("TunnelServer Done!");
    }

    public MySocket getByAddress(InetAddress address) {
        for (MySocket s : sockets) {
            if (s.getSocket().getInetAddress().equals((address))) {
                return s;
            }
        }
        return null;
    }

    public void removeSocket(MySocket socket) {
        sockets.remove(socket);
    }
}
