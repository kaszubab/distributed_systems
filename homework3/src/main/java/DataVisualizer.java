import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class DataVisualizer implements Watcher, AsyncCallback.StatCallback {

    private ZooKeeper zooKeeper;
    private String nodeName;
    private ConnectionManager connectionManager;
    public boolean isRunning = true;

    public DataVisualizer(ZooKeeper zooKeeper, String nodeName, ConnectionManager connectionManager) {
        this.zooKeeper = zooKeeper;
        this.nodeName = nodeName;
        this.connectionManager = connectionManager;
        checkNode();
    }

    private int countChildren(String path) {
        int childrenCount = 0;
        try {
            List<String> children = zooKeeper.getChildren(path, true);
            childrenCount += children.size();
            for(String child: children) {
                childrenCount += countChildren(path + '/' + child);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (KeeperException e) {
            e.printStackTrace();
            return -1;
        }
        return childrenCount;
    };

    private void checkNode() {
        zooKeeper.exists(nodeName, true, this, null);
        int childrenCount = this.countChildren(nodeName);

        if (childrenCount == -1) {
            System.out.println("Node " + nodeName + " doesnt exist");
        }
        else if (childrenCount == 1) {
            System.out.println("There is 1 child");
        }
        else {
            System.out.println("There are " + childrenCount + " children");
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        String path = watchedEvent.getPath();
        //here we handle different event types
        if (watchedEvent.getType() == Event.EventType.None) {
            switch (watchedEvent.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    isRunning = false;
                    connectionManager.closeApp();
                    break;
            }
        } else {
            //here we handle events connected with changing the node
            if (path != null) {
                checkNode();
            }
        }

    }

    @Override
    public void processResult(int i, String path, Object context, Stat stat) {
        boolean exists;
        System.out.println("Code " + i);
        System.out.println("path " + path);
        System.out.println("Keeper  " + KeeperException.Code.get(i));

        switch (KeeperException.Code.get(i)) {
            case OK:
                exists = true;
                break;
            case NONODE:
                exists = false;
                break;
            case SESSIONEXPIRED:
            case NOAUTH:
                isRunning = false;
                connectionManager.closeApp();
                return;
            default:
                zooKeeper.exists(nodeName, true, this, null);
                return;
        }

        connectionManager.connectionStateChanged(exists);
    }
}
