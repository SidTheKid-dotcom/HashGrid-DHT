import java.util.*;

public class NetworkSimulator {
    private List<Node> nodes;
    private final int NUM_NODES = 8;
    private final Random random = new Random();

    private static final int K_BUCKET_SIZE = 3;

    public NetworkSimulator()
    {
        nodes = new ArrayList<>();
        initializeNodes();
    }

    private void initializeNodes() {
        for (int i = 0; i < NUM_NODES; i++) {
            String ip = "127.0.0.1"; // Localhost for simplicity
            int port = 8100 + i; // Assigning unique ports
            int nodeId = random.nextInt(256); // For 8-bit ID space (0-255)
            addNode(ip, port, nodeId);
            System.out.println("Created node: " + ip + ":" + port + " with ID " + nodeId);
        }
    }

    public void addNode(String IP, int UDP_PORT, int NODE_ID) {
        Node newNode = new Node(IP, UDP_PORT, NODE_ID);
        nodes.add(newNode);

        // Update existing node's and newNode's routing tables
        for(Node node : nodes)
        {
            if (node != newNode) {
                node.addToRoutingTable(newNode);
                newNode.addToRoutingTable(node);
            }
        }

        System.out.println("Successfully added node to network node, displaying node ID: "+newNode.getNodeInformation().getNODE_ID());
    }

    public Node findNode(int NODE_ID)
    {
        for(Node node : nodes)
        {
            if(node.getNodeInformation().getNODE_ID() == NODE_ID)
            {
                return node;
            }
        }

        return null;
    }

    public void removeNode(int NODE_ID)
    {
        Node nodeToDelete = findNode(NODE_ID);
        if (nodeToDelete == null) {
            System.out.println("Node ID " + NODE_ID + " not found in network.");
            return;
        }

        // Delete the node
        nodes.remove(nodeToDelete);

        // Update Routing Tables of All Remaining Nodes
        for (Node node : nodes) {
            node.removeFromRoutingTable(NODE_ID);
        }

        // Redistribute Hash Table
        Map<String, Integer> table = nodeToDelete.getHashTable();
        for(Map.Entry<String, Integer> entry : table.entrySet())
        {
            int key = entry.getValue();
            addKey(key);
        }

        System.out.println("Successfully redistributed hash table");
    }

    public void addKey(int key)
    {
        Node node = nodes.get(0);
        Node nearestNode = node.findNearestNode(key);
        if (nearestNode != null) {
            System.out.println("Nearest node to value " + key + " found by Node ID " + node.getNodeInformation().getNODE_ID() +
                    " is Node ID " + nearestNode.getNodeInformation().getNODE_ID());

            nearestNode.addValue(key);
        } else {
            System.out.println("No nearest node found.");
        }
    }

    public int findKey(int searchKey)
    {
        Node node = nodes.get(0);
        return node.findValue(searchKey, new HashSet<Integer>());
    }

    public List<Node> getNodes() {
        return nodes;
    }
}
