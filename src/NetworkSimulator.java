import java.util.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkSimulator {
    private final List<Node> nodes;
    private final int NUM_NODES = 30;
    private final Random random = new Random();
    private static final int K_BUCKET_SIZE = 3;

    public NetworkSimulator() {
        nodes = new ArrayList<>();
        //initializeNodes();
    }

    private void initializeNodes() {
        for (int i = 0; i < NUM_NODES; i++) {
            String ip = "127.0.0.1"; // Localhost for simplicity
            int port = 8100 + i;     // Assigning unique ports
            int nodeId = random.nextInt(256); // For 8-bit ID space (0-255)
            addNode(ip, port, nodeId);
            System.out.println("Created node: " + ip + ":" + port + " with ID " + nodeId);
        }

        initiateNodeDiscovery();
    }

    // Have nodes discover each other through UDP messages
    private void initiateNodeDiscovery() {
        // Allow each node to ping a few random nodes to start building routing tables
        for (Node node : nodes) {
            // Select 3 random nodes to ping
            List<Node> randomNodes = getRandomNodes(node, 3);
            for (Node targetNode : randomNodes) {
                node.sendPing(targetNode);
            }
        }

        // Give nodes time to process messages
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Get random nodes excluding the source node
    private List<Node> getRandomNodes(Node sourceNode, int count) {
        List<Node> availableNodes = new ArrayList<>(nodes);
        availableNodes.remove(sourceNode);

        if (availableNodes.size() <= count) {
            return availableNodes;
        }

        Collections.shuffle(availableNodes, random);
        return availableNodes.subList(0, count);
    }

    public boolean nodeExists(int NODE_ID) {
        return nodes.stream().anyMatch(node -> node.getNodeInformation().getNODE_ID() == NODE_ID);
    }

    public void addNode(String IP, int UDP_PORT, int NODE_ID) {
        if (isPortInUse(UDP_PORT)) {
            System.out.println("Port " + UDP_PORT + " is already in use. Trying alternative port.");
            UDP_PORT = findAvailablePort(UDP_PORT+1);
        }

        Node newNode = new Node(IP, UDP_PORT, NODE_ID, true);
        nodes.add(newNode);

        // Allow some time for the UDP server to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // If there are other nodes, have the new node ping a few to start building its routing table
        if (nodes.size() > 1) {
            List<Node> nodesToPing = getRandomNodes(newNode, 3);
            for (Node targetNode : nodesToPing) {
                newNode.sendPing(targetNode);
            }
        }

        System.out.println("Successfully added node to network, displaying node ID: " + newNode.getNodeInformation().getNODE_ID());
    }

    private boolean isPortInUse(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private int findAvailablePort(int basePort) {
        int port = basePort;
        while (isPortInUse(port)) {
            port++;
            if (port > basePort + 1000) {  // Avoid infinite loop
                throw new RuntimeException("Unable to find available port after 1000 attempts");
            }
        }
        return port;
    }

    public Node findNode(int NODE_ID) {
        for (Node node : nodes) {
            if (node.getNodeInformation().getNODE_ID() == NODE_ID) {
                return node;
            }
        }
        return null;
    }

    public void removeNode(int NODE_ID) {
        Node nodeToDelete = findNode(NODE_ID);
        if (nodeToDelete == null) {
            System.out.println("Node ID " + NODE_ID + " not found in network.");
            return;
        }

        // Before removing the node, notify other nodes that this node is going offline
        // This could be implemented as a GOODBYE message in the Node class

        // Delete the node
        nodes.remove(nodeToDelete);

        // Update Routing Tables of All Remaining Nodes
        for (Node node : nodes) {
            node.removeFromRoutingTable(NODE_ID);
        }

        // Redistribute Hash Table
        Map<String, Integer> table = nodeToDelete.getHashTable();
        System.out.println("Node to delete hash table: "+table);
        for (Map.Entry<String, Integer> entry : table.entrySet()) {
            int key = entry.getValue();
            addKey(key);
        }

        System.out.println("Successfully redistributed hash table");
    }

    public void addKey(int key) {
        if (nodes.isEmpty()) {
            System.out.println("No nodes in the network to add key to.");
            return;
        }

        Node node = nodes.get(0); // Start from the first node
        Triplet nearestNodeInfo = node.findNearestNode(key);

        if (nearestNodeInfo != null) {

            // Retrieve the actual nearest node
            Node nearestNode = nodes.stream()
                    .filter(n -> n.getNodeInformation().getNODE_ID() == nearestNodeInfo.getNODE_ID())
                    .findFirst()
                    .orElse(null);

            if (nearestNode != null) {
                nearestNode.addKey(key);
            } else {
                System.out.println("Error: Nearest node not found in node list.");
            }
        } else {
            System.out.println("No nearest node found.");
        }
    }

    public int findKey(int searchKey) {
        if (nodes.isEmpty()) {
            System.out.println("No nodes in the network to search for the key.");
            return -1;
        }

        Node node = nodes.get(0);
        return node.startFindKey(searchKey);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    // Method to refresh routing tables by having nodes ping their k closest peers
    public void refreshRoutingTables() {
        for (Node node : nodes) {
            node.sendPingKClosest();
        }

        // Give time for pings to be processed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Shutdown the simulator and close all resources
    public void shutdown() {
        // Implement proper socket closure for all nodes
        // For now, we'll rely on the JVM to close resources
        System.out.println("Shutting down simulator with " + nodes.size() + " nodes");
        nodes.clear();
    }
}