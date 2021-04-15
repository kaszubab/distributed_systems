import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class MulticastReceiver implements Runnable {
    private MulticastSocket socket = null;
    private final byte[] buf = new byte[256];
    private final int port;
    private InetAddress group;

    public MulticastReceiver(int port, String multicastAdress){
        this.port = port;
        try {
            group = InetAddress.getByName(multicastAdress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            socket = new MulticastSocket(port);

            socket.joinGroup(group);
            while (true) {
                Arrays.fill(buf, (byte)0);
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData());
                System.out.println(received);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(socket != null) {
                    socket.leaveGroup(group);
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
