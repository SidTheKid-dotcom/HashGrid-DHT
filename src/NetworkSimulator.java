import java.util.*;
import java.io.IOException;
import java.net.DatagramSocket;

public class NetworkSimulator {
    private final List<Node> nodes;
    private final int NUM_NODES = 30;
    private final Random random = new Random();

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

        Node newNode = new Node(IP, UDP_PORT, NODE_ID);
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

    public List<Triplet> lookupNode(int NODE_ID) {
        if (nodes.isEmpty()) {
            System.out.println("No nodes in the network to perform lookup.");
            return new ArrayList<>();
        }

        // Start the lookup from any node in the network
        Node startNode = nodes.get(0);
        return startNode.findNode(NODE_ID);
    }

    public void removeNode(int NODE_ID) {
        List<Triplet> closestNodes = lookupNode(NODE_ID);
        closestNodes.removeIf(node -> node.getNODE_ID() != NODE_ID);

        if (closestNodes.isEmpty()) {
            System.out.println("Node with given Node ID: " + NODE_ID + " not found!");
            return;
        }

        Triplet nodeToDeleteInfo = closestNodes.get(0);

        // Find the node to delete
        Node nodeToDelete = nodes.stream()
                .filter(node -> node.getNodeInformation().getNODE_ID() == nodeToDeleteInfo.getNODE_ID())
                .findFirst()
                .orElse(null);

        if (nodeToDelete == null) {
            System.out.println("Node to delete not found in the network.");
            return;
        }

        // Notify other nodes before deletion
        nodeToDelete.close(); // Assuming such a method exists

        // Delete the node
        nodes.removeIf(node -> node.getNodeInformation().getNODE_ID() == NODE_ID);

        /*Removal of nodes is being adjusted automatically now instead of
        refreshing every node's hash table whenever some random nodes left the network
        refreshRoutingTables();*/

        // Redistribute Hash Table
        Map<String, Integer> table = nodeToDelete.getHashTable();
        System.out.println("Node " + NODE_ID + " to delete hash table: " + table);

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

        // Start from any node
        Node startNode = nodes.get(0);

        // Use node lookup to find the closest nodes
        List<Triplet> closestNodes = startNode.findNode(key);

        if (closestNodes.isEmpty()) {
            System.out.println("No nodes found in the network to store the key.");
            return;
        }

        boolean storedKey = false;

        for (Triplet nodeInfo : closestNodes) {
            Node targetNode = nodes.stream()
                    .filter(n -> n.getNodeInformation().getNODE_ID() == nodeInfo.getNODE_ID())
                    .findFirst()
                    .orElse(null);

            if (targetNode != null) {
                targetNode.storeKey(key);
                storedKey = true;
                System.out.println("Key " + key + " added to node " + targetNode.getNodeInformation().getNODE_ID());
                break;
            }
        }

        if (!storedKey) {
            System.err.println("No available node found to store key: " + key);
        }
    }

    public int findKey(int searchKey) {
        if (nodes.isEmpty()) {
            System.out.println("No nodes in the network to search for the key.");
            return -1;
        }

        Node startNode = nodes.get(0);
        return startNode.findKey(searchKey);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void refreshRoutingTables() {
        for (Node node : nodes) {
            node.sendPingKClosest();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void displayNetworkState() {
        System.out.println("\n===== NETWORK STATE =====");
        System.out.println("Total nodes: " + nodes.size());

        // Display routing tables
        for (Node node : nodes) {
            System.out.println("\nNode ID: " + node.getNodeInformation().getNODE_ID());
            System.out.println("  Hash table entries: " + node.getHashTable().size());

            Map<Integer, List<Triplet>> routingTable = node.getRoutingTable();
            System.out.println("  Routing table buckets: " + routingTable.size());

            // Count total peers
            int totalPeers = routingTable.values().stream()
                    .mapToInt(List::size)
                    .sum();
            System.out.println("  Total known peers: " + totalPeers);
        }
        System.out.println("=======================\n");
    }
    public void testNetworkResilience() {
        System.out.println("\n===== TESTING NETWORK RESILIENCE =====");

        // Add some test keys
        for (int i = 0; i < 10; i++) {
            int testKey = random.nextInt(100);
            addKey(testKey);
            System.out.println("Added test key: " + testKey);
        }

        // Try to find the keys
        for (int i = 0; i < 10; i++) {
            int testKey = i * 10; // Some deterministic keys to search for
            int result = findKey(testKey);
            if (result >= 0) {
                System.out.println("Key " + testKey + " found at node " + result);
            } else {
                System.out.println("Key " + testKey + " not found in network");
            }
        }

        // Remove some random nodes and test again
        if (nodes.size() > 5) {
            System.out.println("\nRemoving 3 random nodes...");
            for (int i = 0; i < 3; i++) {
                if (!nodes.isEmpty()) {
                    int index = random.nextInt(nodes.size());
                    int nodeId = nodes.get(index).getNodeInformation().getNODE_ID();
                    removeNode(nodeId);

                   /* try {
                        Thread.sleep(5000); // Sleep for 1 second (1000 milliseconds)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore the interrupted status
                        System.err.println("Thread was interrupted during sleep");
                    }*/
                }
            }

            // Test finding keys again
            System.out.println("\nRetesting key lookup after node removal:");
            for (int i = 0; i < 10; i++) {
                int testKey = i * 10;
                int result = findKey(testKey);
                if (result >= 0) {
                    System.out.println("Key " + testKey + " found at node " + result);
                } else {
                    System.out.println("Key " + testKey + " not found in network");
                }
            }
        }

        System.out.println("=======================\n");
    }

    // Shutdown the simulator and close all resources
    public void shutdown() {
        for(Node node : nodes)
        {
            node.close();
        }
        System.out.println("Shutting down simulator with " + nodes.size() + " nodes");
        nodes.clear();
    }
}