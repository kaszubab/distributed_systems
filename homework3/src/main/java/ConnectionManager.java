import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.*;
import java.util.List;
import java.util.Scanner;

public class ConnectionManager implements Runnable, Watcher {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("ConnectionManager hostPort znode program [args ...]");
            System.exit(2);
        }

        String port = args[0];
        String nodeName = args[1];
        String[] command = new String[args.length - 2];
        System.arraycopy(args, 2, command, 0, command.length);

        ConnectionManager connectionManager = new ConnectionManager(port, nodeName, command);
        Thread thread = new Thread(connectionManager);
        thread.start();

        Scanner scanner = new Scanner(System.in);

        boolean running = true;
        while (running) {
            String line = scanner.nextLine();
            switch (line) {
                case "print" -> connectionManager.printChildren("/z", 0);
                case "quit" -> {
                    connectionManager.connectionStateChanged(false);
                    connectionManager.closeApp();
                    thread.interrupt();
                    running = false;
                }
            }
        }
    }

    private String nodeName;
    private Process externalProcess;
    private final String[] command;
    private ZooKeeper zooKeeper;
    private DataVisualizer dataVisualizer;

    public ConnectionManager(String port, String nodeName, String[] command) {
        this.nodeName = nodeName;
        this.command = command;
        try {
            zooKeeper = new ZooKeeper("127.0.0.1:" + port, 3000, this);
            dataVisualizer = new DataVisualizer(zooKeeper,nodeName, this);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            synchronized (this) {
                while (dataVisualizer.isRunning) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connectionStateChanged(boolean exists) {
        if (exists) {
            this.startProcess();
        } else {
            this.stopProcess();
        }
    }

    public void closeApp() {
        synchronized (this) {
            notifyAll();
        }
    }


    private void startProcess() {
        if (externalProcess == null) {
            System.out.println("Starting child");
            System.out.println();
            try {
                externalProcess = Runtime.getRuntime().exec(command);
                OutputStream stdin = externalProcess.getOutputStream(); // write to this
                PrintStream printStream = new PrintStream(stdin, true);
                System.setOut(printStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopProcess() {
        if (externalProcess != null) {
            System.out.println("Stopping child");
            externalProcess.destroy();
            try {
                externalProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            externalProcess = null;
        }
    }

    private void printChildren(String path, int indent) {
        try {
            List<String> children = zooKeeper.getChildren(path, this);
            System.out.println("\t".repeat(indent) + path);
            System.out.println();
            children.forEach(child -> {
                printChildren(path + '/' + child, indent + 1);
            });

        }
        catch (KeeperException|InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        dataVisualizer.process(watchedEvent);
    }
}


