import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        System.out.println("JAVA TCP SERVER");
        int portNumber = 12345;
        try {
            Server server = new Server(portNumber);
            server.run();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
