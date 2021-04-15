import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Server {

    private final List<ClientConnection> clientConnections = new LinkedList<>();
    private ServerSocket serverSocketTcp;
    private DatagramSocket serverSocketUdp;
    private final byte[] receiveBufferUdp = new byte[1024 * 16];
    private final int portNumber;

    public Server(int portNumber) {
        this.portNumber = portNumber;
    }

    public void run() throws IOException {
        try {
            // create socket
            serverSocketTcp = new ServerSocket(portNumber);
            serverSocketUdp = new DatagramSocket(portNumber);
            MulticastReceiver multicastReceiver = new MulticastReceiver(4004, "230.0.0.0");

            Thread multicastListener = new Thread(multicastReceiver);
            Thread tcpListener = setupTcpListener(serverSocketTcp);
            Thread udpListener = setupUdpListener(serverSocketUdp);

            tcpListener.start();
            udpListener.start();
            multicastListener.start();

            while(true) {
                Scanner scanner = new Scanner(System.in);
                if (scanner.nextLine().equals("STOP")) {
                   tcpListener.interrupt();
                   udpListener.interrupt();
                   multicastListener.interrupt();
                   break;
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            if (serverSocketTcp != null) serverSocketTcp.close();
            if (serverSocketUdp != null) serverSocketUdp.close();
        }

    }

    private void handleClient(Socket clientSocket) throws IOException {
        ClientConnection clientConnection = new ClientConnection(clientSocket, this);
        if(clientConnection.nickname == null) return;
        clientConnections.add(clientConnection);
        Thread thread = new Thread(clientConnection);
        thread.start();
    }

    private Thread setupTcpListener(ServerSocket serverSocketTcp) {
        return new Thread(() -> {
            try {
                while(true){
                    Socket clientSocket = serverSocketTcp.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Thread setupUdpListener(DatagramSocket serverSocketUdp) {
        return new Thread(() -> {
            try {
                while(true){
                    Arrays.fill(receiveBufferUdp, (byte)0);
                    DatagramPacket receivePacket = new DatagramPacket(receiveBufferUdp, receiveBufferUdp.length);
                    serverSocketUdp.receive(receivePacket);
                    sendMessageToClientsUdp(receivePacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendMessageToClientsTcp(String message, String nickname) {
        clientConnections.forEach(client -> {
            if(!client.nickname.equals(nickname)) client.sendMessageToClientTCP(message);
        });
    }

    public void sendMessageToClientsUdp(DatagramPacket receivePacket) {
        clientConnections.forEach(client -> {
            if(client.getAdress().equals(receivePacket.getAddress()) &&
                    client.getPort() == receivePacket.getPort()) return;
            try {
                DatagramPacket datagramPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getData().length,
                        client.getAdress(), client.getPort());
                serverSocketUdp.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void removeClient(String nickname) {
        clientConnections.removeIf(client -> client.nickname.equals(nickname));
    }

}
