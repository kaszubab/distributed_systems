import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {

    public static void main(String[] args) throws IOException {

        String hostName = "localhost";
        int portNumber = 12345;
        Socket socketTcp = null;
        DatagramSocket socketUdp = null;
        MulticastSender multicastSender = new MulticastSender();


        try {
            // create socket
            socketTcp = new Socket(hostName, portNumber);
            socketUdp = new DatagramSocket(socketTcp.getLocalPort());
            InetAddress address = InetAddress.getByName("localhost");

            // in & out streams
            PrintWriter out = new PrintWriter(socketTcp.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socketTcp.getInputStream()));

            TCPReceiver tcpReceiver = new TCPReceiver(socketTcp, in);
            UDPReceiver udpReceiver = new UDPReceiver(socketUdp, (DatagramPacket packet) -> {
                String data = new String(packet.getData());
                System.out.println(data);
            });
            MulticastReceiver receiver = new MulticastReceiver(4004, "230.0.0.0");


            Thread tcpListener = new Thread(tcpReceiver);
            Thread udpListener = new Thread(udpReceiver);
            Thread multicastListener = new Thread(receiver);

            multicastListener.start();
            tcpListener.start();
            udpListener.start();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter nickname");
            String nickname = scanner.nextLine();
            out.println(nickname);

            boolean running = true;
            while (running) {
                System.out.println("Enter mode T - tcp, U - udp, M - multicast udp, STOP -> to stop");
                String mode = scanner.nextLine();
                String message = "";
                if(!mode.equals("STOP")) {
                    System.out.println("Enter message");
                    message = scanner.nextLine();
                }
                if(socketUdp.isClosed() || socketTcp.isClosed()) break;
                switch (mode) {
                    case "T" -> {
                        out.println(message);
                        System.out.println("Sending message " + message + " by " + mode);
                    }
                    case "U" -> {
                        DatagramPacket datagramPacket = new DatagramPacket((nickname + " " + message).getBytes(),
                                (nickname + " " + message).getBytes().length,
                                address, portNumber);
                        socketUdp.send(datagramPacket);
                        System.out.println("Sending message " + message + " by " + mode);
                    }
                    case "M" -> {
                        System.out.println("Enter multicast group");
                        String multicastGroup = scanner.nextLine();
                        multicastSender.send((nickname + " " + message), multicastGroup, 4004);
                    }
                    case "STOP" -> {
                        running = false;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socketTcp != null && !socketTcp.isClosed()){
                socketTcp.close();
            }
            if (socketUdp != null){
                socketUdp.close();
            }
        }
    }
}
