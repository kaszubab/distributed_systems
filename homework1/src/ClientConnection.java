import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.Buffer;

public class ClientConnection implements Runnable{

    Socket socket;
    PrintWriter out;
    BufferedReader in;
    String nickname;
    Server server;

    public ClientConnection(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.nickname = in.readLine();
    }

    public InetAddress getAdress() {
        return this.socket.getInetAddress();
    }

    public int getPort() {
        return this.socket.getPort();
    }

    public void sendMessageToClientTCP(String message) {
        out.println(message);
    }

    public void sendMessageToClientUdp(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = in.readLine();
                server.sendMessageToClientsTcp(nickname + " " + message, this.nickname);
            }
        }
        catch (SocketException e) {
            server.removeClient(nickname);
        }
        catch (IOException e) {
            System.out.println("IO exception " + socket.isClosed());
            e.printStackTrace();
        }

    }
}
