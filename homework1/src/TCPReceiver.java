import java.io.BufferedReader;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class TCPReceiver implements Runnable {
    private final Socket socket;
    private final BufferedReader in;

    public TCPReceiver(Socket socket, BufferedReader in){
        this.socket = socket;
        this.in = in;
    }

    public void run() {
        try {
            while (true) {
                System.out.println("received response: " + in.readLine());
            }
        } catch (SocketException e) {
            try {
                if (socket != null) socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
