import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class TunnelClient extends Thread {
    private MySocket mySocket;
    private DatagramSocket mUdpSocket;

    private String address;
    private int port;

    public TunnelClient(String address, int port) {
        this.address = address;
        this.port = port;
        init();
    }

    public void init(){
        try {
            Socket socket = new Socket(InetAddress.getByName(address), port);
            mySocket = new MySocket(socket);
            mUdpSocket = new DatagramSocket();
            System.out.println("client connect to " + address + " " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (!isInterrupted()) {
            if (!mySocket.isConnected()){
                init();
            }

            DatagramPacket packet = mySocket.recv();
            if(packet == null){
                init();
                continue;
            }

            try {
                mUdpSocket.send(packet);
                System.out.println("send packet to port " + packet.getPort() + " sized " + packet.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("client end");
    }
}
