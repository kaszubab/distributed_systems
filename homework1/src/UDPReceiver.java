import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class UDPReceiver implements Runnable {
    private DatagramSocket socket;
    private final byte[] buf = new byte[1024*16];
    private final Consumer<DatagramPacket> onReceive;

    public UDPReceiver(DatagramSocket socket, Consumer<DatagramPacket> onReceive){
        this.socket = socket;
        this.onReceive = onReceive;
    }

    public void run() {
        try {
            while (true) {
                Arrays.fill(buf, (byte)0);
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                onReceive.accept(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null) socket.close();
        }
    }
}
