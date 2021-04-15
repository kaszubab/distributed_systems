import java.io.IOException;
import java.net.*;

public class MulticastSender {
    private DatagramSocket socket = null;
    private byte[] buf = new byte[1024 * 16];
    private InetAddress group;

    public MulticastSender(){
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

    public void send(String multicastMessage, String multicastAdress, int port) {
        try {
            group = InetAddress.getByName(multicastAdress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            buf = multicastMessage.getBytes();
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length, group, port);
            socket.send(packet);
        } catch (IOException e) {
            socket.close();
        }
    }

    public void closeSocket() {
        if(!socket.isClosed()) socket.close();
    }
}